package proton.android.pass.featureitemcreate.impl.login

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import proton.android.pass.featureitemcreate.impl.bottomsheets.customfield.customFieldBottomSheetGraph
import proton.android.pass.featureitemcreate.impl.dialogs.CustomFieldNameNavigation
import proton.android.pass.featureitemcreate.impl.dialogs.customFieldNameDialogGraph
import proton.android.pass.featureitemcreate.impl.login.bottomsheet.aliasoptions.aliasOptionsBottomSheetGraph
import proton.android.pass.featureitemcreate.impl.totp.TOTP_NAV_PARAMETER_KEY
import proton.android.pass.navigation.api.CommonNavArgId
import proton.android.pass.navigation.api.NavItem
import proton.android.pass.navigation.api.composable
import proton.pass.domain.ItemId
import proton.pass.domain.ShareId

object EditLogin : NavItem(
    baseRoute = "login/edit",
    navArgIds = listOf(CommonNavArgId.ShareId, CommonNavArgId.ItemId)
) {
    fun createNavRoute(shareId: ShareId, itemId: ItemId) =
        "$baseRoute/${shareId.id}/${itemId.id}"
}

@Suppress("LongParameterList")
@OptIn(ExperimentalAnimationApi::class, ExperimentalLifecycleComposeApi::class)
fun NavGraphBuilder.updateLoginGraph(
    onNavigate: (BaseLoginNavigation) -> Unit
) {
    composable(EditLogin) { navBackStack ->
        val primaryTotp by navBackStack.savedStateHandle
            .getStateFlow<String?>(TOTP_NAV_PARAMETER_KEY, null)
            .collectAsStateWithLifecycle()
        UpdateLogin(
            draftAlias = null,
            primaryTotp = primaryTotp,
            onNavigate = onNavigate
        )
    }

    aliasOptionsBottomSheetGraph(onNavigate)
    customFieldBottomSheetGraph(onNavigate)
    customFieldNameDialogGraph {
        when (it) {
            is CustomFieldNameNavigation.Close -> {
                onNavigate(BaseLoginNavigation.Close)
            }
        }
    }
}
