package proton.android.pass.featureitemdetail.impl.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import proton.android.pass.clipboard.api.ClipboardManager
import proton.android.pass.common.api.LoadingResult
import proton.android.pass.common.api.None
import proton.android.pass.common.api.Option
import proton.android.pass.common.api.Some
import proton.android.pass.common.api.asLoadingResult
import proton.android.pass.common.api.combineN
import proton.android.pass.common.api.getOrNull
import proton.android.pass.common.api.some
import proton.android.pass.common.api.toOption
import proton.android.pass.commonui.api.require
import proton.android.pass.commonui.api.toUiModel
import proton.android.pass.composecomponents.impl.uievents.IsLoadingState
import proton.android.pass.composecomponents.impl.uievents.IsPermanentlyDeletedState
import proton.android.pass.composecomponents.impl.uievents.IsRestoredFromTrashState
import proton.android.pass.composecomponents.impl.uievents.IsSentToTrashState
import proton.android.pass.crypto.api.context.EncryptionContextProvider
import proton.android.pass.data.api.usecases.CanDisplayTotp
import proton.android.pass.data.api.usecases.DeleteItem
import proton.android.pass.data.api.usecases.GetItemByAliasEmail
import proton.android.pass.data.api.usecases.GetItemByIdWithVault
import proton.android.pass.data.api.usecases.ItemWithVaultInfo
import proton.android.pass.data.api.usecases.ObserveUpgradeInfo
import proton.android.pass.data.api.usecases.RestoreItem
import proton.android.pass.data.api.usecases.TrashItem
import proton.android.pass.featureitemdetail.impl.DetailSnackbarMessages.InitError
import proton.android.pass.featureitemdetail.impl.DetailSnackbarMessages.ItemMovedToTrash
import proton.android.pass.featureitemdetail.impl.DetailSnackbarMessages.ItemNotMovedToTrash
import proton.android.pass.featureitemdetail.impl.DetailSnackbarMessages.ItemNotPermanentlyDeleted
import proton.android.pass.featureitemdetail.impl.DetailSnackbarMessages.ItemNotRestored
import proton.android.pass.featureitemdetail.impl.DetailSnackbarMessages.ItemPermanentlyDeleted
import proton.android.pass.featureitemdetail.impl.DetailSnackbarMessages.ItemRestored
import proton.android.pass.featureitemdetail.impl.DetailSnackbarMessages.PasswordCopiedToClipboard
import proton.android.pass.featureitemdetail.impl.DetailSnackbarMessages.TotpCopiedToClipboard
import proton.android.pass.featureitemdetail.impl.DetailSnackbarMessages.UsernameCopiedToClipboard
import proton.android.pass.featureitemdetail.impl.DetailSnackbarMessages.WebsiteCopiedToClipboard
import proton.android.pass.featureitemdetail.impl.ItemDelete
import proton.android.pass.log.api.PassLogger
import proton.android.pass.navigation.api.CommonNavArgId
import proton.android.pass.notifications.api.SnackbarDispatcher
import proton.android.pass.telemetry.api.EventItemType
import proton.android.pass.telemetry.api.TelemetryManager
import proton.android.pass.totp.api.ObserveTotpFromUri
import proton.android.pass.totp.api.TotpManager
import proton.pass.domain.Item
import proton.pass.domain.ItemId
import proton.pass.domain.ItemType
import proton.pass.domain.ShareId
import proton.pass.domain.Vault
import javax.inject.Inject

@HiltViewModel
class LoginDetailViewModel @Inject constructor(
    private val snackbarDispatcher: SnackbarDispatcher,
    private val clipboardManager: ClipboardManager,
    private val encryptionContextProvider: EncryptionContextProvider,
    private val observeTotpFromUri: ObserveTotpFromUri,
    private val trashItem: TrashItem,
    private val deleteItem: DeleteItem,
    private val restoreItem: RestoreItem,
    private val getItemByAliasEmail: GetItemByAliasEmail,
    private val telemetryManager: TelemetryManager,
    private val canDisplayTotp: CanDisplayTotp,
    observeUpgradeInfo: ObserveUpgradeInfo,
    getItemByIdWithVault: GetItemByIdWithVault,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val shareId: ShareId = ShareId(savedStateHandle.require(CommonNavArgId.ShareId.key))
    private val itemId: ItemId = ItemId(savedStateHandle.require(CommonNavArgId.ItemId.key))

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        PassLogger.e(TAG, throwable)
    }

    private val passwordState: MutableStateFlow<PasswordState> =
        MutableStateFlow(getInitialPasswordState())
    private val isLoadingState: MutableStateFlow<IsLoadingState> =
        MutableStateFlow(IsLoadingState.NotLoading)
    private val isItemSentToTrashState: MutableStateFlow<IsSentToTrashState> =
        MutableStateFlow(IsSentToTrashState.NotSent)
    private val isPermanentlyDeletedState: MutableStateFlow<IsPermanentlyDeletedState> =
        MutableStateFlow(IsPermanentlyDeletedState.NotDeleted)
    private val isRestoredFromTrashState: MutableStateFlow<IsRestoredFromTrashState> =
        MutableStateFlow(IsRestoredFromTrashState.NotRestored)

    private val itemDetailsFlow: Flow<LoadingResult<LoginItemInfo>> =
        getItemByIdWithVault(shareId, itemId)
            .asLoadingResult()
            .flatMapLatest { res ->
                val details = when (res) {
                    LoadingResult.Loading -> return@flatMapLatest flowOf(LoadingResult.Loading)
                    is LoadingResult.Error -> {
                        PassLogger.e(TAG, res.exception, "Error loading item")
                        return@flatMapLatest flowOf(res)
                    }

                    is LoadingResult.Success -> res.data
                }

                val itemContents = details.item.itemType as ItemType.Login
                val alias = getAliasForItem(itemContents)

                val decryptedTotpUri = encryptionContextProvider.withEncryptionContext {
                    decrypt(itemContents.primaryTotp)
                }

                if (decryptedTotpUri.isNotEmpty()) {
                    observeTotp(
                        details = details,
                        alias = alias,
                        totpUri = decryptedTotpUri,
                    ).asLoadingResult()
                } else {
                    flowOf(
                        LoadingResult.Success(
                            LoginItemInfo(
                                item = details.item,
                                totp = None,
                                vault = details.vault,
                                hasMoreThanOneVault = details.hasMoreThanOneVault,
                                linkedAlias = alias
                            )
                        )
                    )
                }
            }
            .distinctUntilChanged()

    private data class LoginItemInfo(
        val item: Item,
        val totp: Option<TotpUiState>,
        val vault: Vault,
        val hasMoreThanOneVault: Boolean,
        val linkedAlias: Option<LinkedAliasItem>
    )

    val uiState: StateFlow<LoginDetailUiState> = combineN(
        itemDetailsFlow,
        passwordState,
        isLoadingState,
        isItemSentToTrashState,
        isPermanentlyDeletedState,
        isRestoredFromTrashState,
        observeUpgradeInfo().asLoadingResult()
    ) { itemDetails,
        password,
        isLoading,
        isItemSentToTrash,
        isPermanentlyDeleted,
        isRestoredFromTrash,
        upgradeInfoResult ->
        when (itemDetails) {
            is LoadingResult.Error -> {
                snackbarDispatcher(InitError)
                LoginDetailUiState.Error
            }

            LoadingResult.Loading -> LoginDetailUiState.NotInitialised
            is LoadingResult.Success -> {
                val details = itemDetails.data
                val vault = if (details.hasMoreThanOneVault) {
                    details.vault
                } else {
                    null
                }
                val canMigrate =
                    if (upgradeInfoResult.getOrNull()?.isUpgradeAvailable == true) {
                        !(vault?.isPrimary ?: false)
                    } else {
                        true
                    }
                LoginDetailUiState.Success(
                    itemUiModel = encryptionContextProvider.withEncryptionContext {
                        details.item.toUiModel(this)
                    },
                    vault = vault,
                    linkedAlias = details.linkedAlias,
                    passwordState = password,
                    totpUiState = details.totp.value(),
                    isLoading = isLoading.value(),
                    isItemSentToTrash = isItemSentToTrash.value(),
                    isPermanentlyDeleted = isPermanentlyDeleted.value(),
                    isRestoredFromTrash = isRestoredFromTrash.value(),
                    canMigrate = canMigrate
                )
            }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LoginDetailUiState.NotInitialised
        )

    fun copyPasswordToClipboard() = viewModelScope.launch(coroutineExceptionHandler) {
        val state = uiState.value as? LoginDetailUiState.Success ?: return@launch
        val itemType = state.itemUiModel.itemType as? ItemType.Login ?: return@launch
        val text = when (val password = passwordState.value) {
            is PasswordState.Revealed -> password.clearText
            is PasswordState.Concealed -> encryptionContextProvider.withEncryptionContext {
                decrypt(itemType.password)
            }
        }
        clipboardManager.copyToClipboard(text = text, isSecure = true)
        snackbarDispatcher(PasswordCopiedToClipboard)
    }

    fun copyUsernameToClipboard() = viewModelScope.launch {
        val state = uiState.value as? LoginDetailUiState.Success ?: return@launch
        val itemType = state.itemUiModel.itemType as? ItemType.Login ?: return@launch
        clipboardManager.copyToClipboard(itemType.username)
        snackbarDispatcher(UsernameCopiedToClipboard)
    }

    fun copyWebsiteToClipboard(website: String) = viewModelScope.launch {
        clipboardManager.copyToClipboard(website)
        snackbarDispatcher(WebsiteCopiedToClipboard)
    }

    fun copyTotpCodeToClipboard(code: String) = viewModelScope.launch {
        clipboardManager.copyToClipboard(code)
        snackbarDispatcher(TotpCopiedToClipboard)
    }

    fun togglePassword() = viewModelScope.launch(coroutineExceptionHandler) {
        val state = uiState.value as? LoginDetailUiState.Success ?: return@launch
        val itemType = state.itemUiModel.itemType as? ItemType.Login ?: return@launch

        when (passwordState.value) {
            is PasswordState.Concealed ->
                encryptionContextProvider.withEncryptionContext {
                    passwordState.value = PasswordState.Revealed(
                        encrypted = itemType.password,
                        clearText = decrypt(itemType.password)
                    )
                }

            is PasswordState.Revealed ->
                passwordState.value = PasswordState.Concealed(itemType.password)
        }
    }

    fun onMoveToTrash(shareId: ShareId, itemId: ItemId) = viewModelScope.launch {
        isLoadingState.update { IsLoadingState.Loading }
        runCatching { trashItem(shareId = shareId, itemId = itemId) }
            .onSuccess {
                isItemSentToTrashState.update { IsSentToTrashState.Sent }
                snackbarDispatcher(ItemMovedToTrash)
            }
            .onFailure {
                snackbarDispatcher(ItemNotMovedToTrash)
                PassLogger.d(TAG, it, "Could not delete item")
            }
        isLoadingState.update { IsLoadingState.NotLoading }
    }

    fun onPermanentlyDelete(shareId: ShareId, itemId: ItemId, itemType: ItemType) =
        viewModelScope.launch {
            isLoadingState.update { IsLoadingState.Loading }
            runCatching {
                deleteItem(shareId = shareId, itemId = itemId)
            }.onSuccess {
                telemetryManager.sendEvent(ItemDelete(EventItemType.from(itemType)))
                isPermanentlyDeletedState.update { IsPermanentlyDeletedState.Deleted }
                snackbarDispatcher(ItemPermanentlyDeleted)
                PassLogger.i(TAG, "Item deleted successfully")
            }.onFailure {
                snackbarDispatcher(ItemNotPermanentlyDeleted)
                PassLogger.i(TAG, it, "Could not delete item")
            }
            isLoadingState.update { IsLoadingState.NotLoading }
        }

    fun onItemRestore(shareId: ShareId, itemId: ItemId) = viewModelScope.launch {
        isLoadingState.update { IsLoadingState.Loading }
        runCatching {
            restoreItem(shareId = shareId, itemId = itemId)
        }.onSuccess {
            isRestoredFromTrashState.update { IsRestoredFromTrashState.Restored }
            PassLogger.i(TAG, "Item restored successfully")
            snackbarDispatcher(ItemRestored)
        }.onFailure {
            PassLogger.i(TAG, it, "Error restoring item")
            snackbarDispatcher(ItemNotRestored)
        }
        isLoadingState.update { IsLoadingState.NotLoading }
    }

    private fun observeTotp(
        details: ItemWithVaultInfo,
        alias: Option<LinkedAliasItem>,
        totpUri: String
    ): Flow<LoginItemInfo> = canDisplayTotp(shareId = shareId, itemId = itemId)
        .flatMapLatest { canDisplay ->
            if (canDisplay) {
                observeTotpValue(details, alias, totpUri)
            } else {
                flowOf(
                    LoginItemInfo(
                        item = details.item,
                        totp = TotpUiState.Hidden.some(),
                        vault = details.vault,
                        hasMoreThanOneVault = details.hasMoreThanOneVault,
                        linkedAlias = alias
                    )
                )

            }
        }

    private fun observeTotpValue(
        details: ItemWithVaultInfo,
        alias: Option<LinkedAliasItem>,
        totpUri: String
    ): Flow<LoginItemInfo> = observeTotpFromUri(totpUri)
        .map(TotpManager.TotpWrapper::toOption)
        .catch { e ->
            PassLogger.w(TAG, e, "Error observing totp")
            LoginItemInfo(
                item = details.item,
                totp = None,
                vault = details.vault,
                hasMoreThanOneVault = details.hasMoreThanOneVault,
                linkedAlias = alias
            )
        }
        .map { totpValue ->
            val totp = totpValue.map {
                TotpUiState.Visible(it.code, it.remainingSeconds, it.totalSeconds)
            }
            LoginItemInfo(
                item = details.item,
                totp = totp,
                vault = details.vault,
                hasMoreThanOneVault = details.hasMoreThanOneVault,
                linkedAlias = alias
            )

        }


    private fun getInitialPasswordState(): PasswordState =
        encryptionContextProvider.withEncryptionContext {
            PasswordState.Concealed(encrypt(""))
        }

    private suspend fun getAliasForItem(item: ItemType.Login): Option<LinkedAliasItem> {
        val username = item.username
        if (username.isBlank()) return None

        return runCatching { getItemByAliasEmail(aliasEmail = username) }
            .fold(
                onSuccess = {
                    if (it == null) {
                        None
                    } else {
                        Some(LinkedAliasItem(shareId = it.shareId, itemId = it.id))
                    }
                },
                onFailure = {
                    PassLogger.w(TAG, it, "Error fetching alias for item")
                    None
                }
            )
    }

    companion object {
        private const val TAG = "LoginDetailViewModel"
    }
}
