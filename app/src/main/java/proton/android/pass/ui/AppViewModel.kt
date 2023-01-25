package proton.android.pass.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.encrypt
import me.proton.core.domain.entity.UserId
import proton.android.pass.R
import proton.android.pass.common.api.Result
import proton.android.pass.common.api.asResultWithoutLoading
import proton.android.pass.common.api.onError
import proton.android.pass.common.api.onSuccess
import proton.android.pass.commonuimodels.api.ShareUiModel
import proton.android.pass.data.api.ItemCountSummary
import proton.android.pass.data.api.repositories.ItemRepository
import proton.android.pass.data.api.usecases.ApplyPendingEvents
import proton.android.pass.data.api.usecases.CreateVault
import proton.android.pass.data.api.usecases.GetCurrentShare
import proton.android.pass.data.api.usecases.GetCurrentUserId
import proton.android.pass.data.api.usecases.ObserveCurrentUser
import proton.android.pass.data.api.usecases.ObserveVaults
import proton.android.pass.data.api.usecases.RefreshShares
import proton.android.pass.log.api.PassLogger
import proton.android.pass.network.api.NetworkMonitor
import proton.android.pass.network.api.NetworkStatus
import proton.android.pass.notifications.api.SnackbarMessageRepository
import proton.android.pass.preferences.ThemePreference
import proton.android.pass.preferences.UserPreferencesRepository
import proton.android.pass.presentation.navigation.drawer.DrawerUiState
import proton.android.pass.presentation.navigation.drawer.ItemTypeSection
import proton.android.pass.presentation.navigation.drawer.NavigationDrawerSection
import proton.android.pass.ui.AppSnackbarMessage.CouldNotRefreshItems
import proton.pass.domain.Share
import proton.pass.domain.entity.NewVault
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUser,
    preferenceRepository: UserPreferencesRepository,
    observeVaults: ObserveVaults,
    itemRepository: ItemRepository,
    networkMonitor: NetworkMonitor,
    private val getCurrentUserId: GetCurrentUserId,
    private val getCurrentShare: GetCurrentShare,
    private val createVault: CreateVault,
    private val refreshShares: RefreshShares,
    private val cryptoContext: CryptoContext,
    private val snackbarMessageRepository: SnackbarMessageRepository,
    private val applyPendingEvents: ApplyPendingEvents
) : ViewModel() {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        PassLogger.e(TAG, throwable)
    }

    private val currentUserFlow = observeCurrentUser().filterNotNull()
    private val drawerSectionState: MutableStateFlow<NavigationDrawerSection> =
        MutableStateFlow(NavigationDrawerSection.AllItems())

    private val themePreference: Flow<ThemePreference> = preferenceRepository
        .getThemePreference()
        .asResultWithoutLoading()
        .map { getThemePreference(it) }

    private val allShareUiModelFlow: Flow<List<ShareUiModel>> = observeVaults()
        .map { shares ->
            when (shares) {
                Result.Loading -> emptyList()
                is Result.Error -> {
                    val message = "Cannot retrieve all shares"
                    PassLogger.e(TAG, shares.exception ?: Exception(message), message)
                    emptyList()
                }
                is Result.Success ->
                    shares.data
                        .map { ShareUiModel(it.shareId, it.name) }
            }
        }
        .distinctUntilChanged()

    private val itemCountSummaryFlow = combine(
        currentUserFlow,
        drawerSectionState,
        allShareUiModelFlow
    ) { user, drawerSection, allShares ->
        val shares: List<ShareUiModel> = when (drawerSection) {
            is ItemTypeSection ->
                drawerSection.shareId
                    ?.let { selectedShare ->
                        allShares.filter { share -> share.id == selectedShare }
                    }
                    ?: allShares
            else -> allShares
        }
        user to shares
    }
        .flatMapLatest { pair ->
            itemRepository.observeItemCountSummary(pair.first.userId, pair.second.map { it.id })
        }

    private val drawerStateFlow: Flow<DrawerState> = combine(
        drawerSectionState,
        itemCountSummaryFlow,
        allShareUiModelFlow
    ) { drawerSection, itemCountSummary, shares ->
        DrawerState(
            section = drawerSection,
            itemCountSummary = itemCountSummary,
            shares = shares
        )
    }

    private data class DrawerState(
        val section: NavigationDrawerSection,
        val itemCountSummary: ItemCountSummary,
        val shares: List<ShareUiModel>
    )

    private val networkStatus: Flow<NetworkStatus> = networkMonitor
        .connectivity
        .distinctUntilChanged()

    val appUiState: StateFlow<AppUiState> = combine(
        currentUserFlow,
        drawerStateFlow,
        snackbarMessageRepository.snackbarMessage,
        themePreference,
        networkStatus
    ) { user, drawerState, snackbarMessage, theme, network ->
        AppUiState(
            snackbarMessage = snackbarMessage,
            drawerUiState = DrawerUiState(
                appNameResId = R.string.app_name,
                currentUser = user,
                selectedSection = drawerState.section,
                itemCountSummary = drawerState.itemCountSummary,
                shares = drawerState.shares
            ),
            theme = theme,
            networkStatus = network
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppUiState.Initial(
                runBlocking {
                    preferenceRepository.getThemePreference().first()
                }
            )
        )

    fun onStart() = viewModelScope.launch(coroutineExceptionHandler) {
        val userIdResult: Result<UserId> = getCurrentUserId()
        userIdResult
            .onSuccess { userId ->
                refreshShares(userId)
                    .onError { onInitError(it, "Refresh shares error") }
                getCurrentShare(userId)
                    .onSuccess { onShareListReceived(it, userId) }
                    .onError { onInitError(it, "Observe shares error") }
            }
            .onError { onInitError(it, "UserId error") }
    }


    private suspend fun onShareListReceived(
        list: List<Share>,
        userId: UserId
    ) {
        if (list.isEmpty()) {
            val vault = NewVault(
                name = "Personal".encrypt(cryptoContext.keyStoreCrypto),
                description = "Personal vault".encrypt(cryptoContext.keyStoreCrypto)
            )
            createVault(userId, vault)
                .onError { onInitError(it, "Create Vault error") }
        } else {
            applyEvents()
        }
    }

    private fun applyEvents() = viewModelScope.launch {
        applyPendingEvents()
            .onError { t ->
                PassLogger.e(TAG, t ?: Exception("Apply pending events failed"))
                snackbarMessageRepository.emitSnackbarMessage(CouldNotRefreshItems)
            }
    }

    private suspend fun onInitError(throwable: Throwable?, message: String) {
        PassLogger.e(TAG, throwable ?: Exception(message), message)
        snackbarMessageRepository.emitSnackbarMessage(AppSnackbarMessage.ErrorDuringStartup)
    }

    fun onDrawerSectionChanged(drawerSection: NavigationDrawerSection) {
        drawerSectionState.update { drawerSection }
    }

    fun onSnackbarMessageDelivered() {
        viewModelScope.launch {
            snackbarMessageRepository.snackbarMessageDelivered()
        }
    }

    private fun getThemePreference(state: Result<ThemePreference>): ThemePreference =
        when (state) {
            Result.Loading -> ThemePreference.System
            is Result.Success -> state.data
            is Result.Error -> {
                val message = "Error getting ThemePreference"
                PassLogger.w(TAG, state.exception ?: Exception(message))
                ThemePreference.System
            }
        }

    companion object {
        private const val TAG = "AppViewModel"
    }
}
