package proton.android.pass.featureitemcreate.impl.login

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import kotlinx.coroutines.flow.StateFlow
import proton.android.pass.common.api.None
import proton.android.pass.common.api.Option
import proton.android.pass.common.api.Some
import proton.android.pass.featureitemcreate.impl.login.bottomsheet.aliasoptions.CLEAR_ALIAS_NAV_PARAMETER_KEY
import proton.android.pass.featureitemcreate.impl.login.bottomsheet.aliasoptions.aliasOptionsBottomSheetGraph
import proton.android.pass.navigation.api.CommonOptionalNavArgId
import proton.android.pass.navigation.api.NavItem
import proton.android.pass.navigation.api.OptionalNavArgId
import proton.android.pass.navigation.api.composable
import proton.android.pass.navigation.api.toPath
import proton.pass.domain.ShareId

object CreateLoginDefaultUsernameArg : OptionalNavArgId {
    override val key = "username"
    override val navType = NavType.StringType
}

object CreateLogin : NavItem(
    baseRoute = "login/create",
    optionalArgIds = listOf(CommonOptionalNavArgId.ShareId, CreateLoginDefaultUsernameArg)
) {
    fun createNavRoute(
        shareId: Option<ShareId> = None,
        username: Option<String> = None
    ) = buildString {
        append(baseRoute)
        val map = mutableMapOf<String, Any>()
        if (shareId is Some) {
            map[CommonOptionalNavArgId.ShareId.key] = shareId.value.id
        }
        if (username is Some) {
            map[CreateLoginDefaultUsernameArg.key] = username.value
        }
        val path = map.toPath()
        append(path)
    }
}

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalLifecycleComposeApi::class
)
fun NavGraphBuilder.createLoginGraph(
    initialCreateLoginUiState: InitialCreateLoginUiState = InitialCreateLoginUiState(),
    showCreateAliasButton: Boolean = true,
    getPrimaryTotp: () -> StateFlow<String?>,
    onNavigate: (BaseLoginNavigation) -> Unit
) {
    composable(CreateLogin) {
        val primaryTotp by getPrimaryTotp().collectAsStateWithLifecycle()
        val clearAlias by it.savedStateHandle.getStateFlow(CLEAR_ALIAS_NAV_PARAMETER_KEY, false)
            .collectAsStateWithLifecycle()
        val initialContents = initialCreateLoginUiState.copy(
            primaryTotp = primaryTotp
        )

        CreateLoginScreen(
            initialContents = initialContents,
            clearAlias = clearAlias,
            showCreateAliasButton = showCreateAliasButton,
            onNavigate = onNavigate
        )
    }

    aliasOptionsBottomSheetGraph(onNavigate)
}
