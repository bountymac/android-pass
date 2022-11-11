package me.proton.android.pass.ui.create.alias

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.NavGraphBuilder
import me.proton.android.pass.navigation.api.AppNavigator
import me.proton.android.pass.ui.navigation.AppNavItem
import me.proton.android.pass.ui.navigation.composable
import me.proton.pass.presentation.create.alias.CreateAlias
import me.proton.pass.presentation.create.alias.RESULT_CREATED_ALIAS

@OptIn(
    ExperimentalAnimationApi::class, ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
fun NavGraphBuilder.createAliasGraph(nav: AppNavigator) {
    composable(AppNavItem.CreateAlias) {
        CreateAlias(
            onClose = { nav.onBackClick() },
            onUpClick = { nav.onBackClick() },
            onSuccess = { alias ->
                nav.navigateUpWithResult(RESULT_CREATED_ALIAS, alias)
            }
        )
    }
}
