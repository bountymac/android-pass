package me.proton.android.pass.ui.detail

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import me.proton.android.pass.navigation.api.AppNavigator
import me.proton.android.pass.navigation.api.composable
import me.proton.android.pass.ui.navigation.AppNavItem
import me.proton.pass.domain.ItemType
import me.proton.pass.presentation.detail.ItemDetailScreen

@OptIn(
    ExperimentalAnimationApi::class
)
fun NavGraphBuilder.itemDetailGraph(modifier: Modifier, nav: AppNavigator) {
    composable(AppNavItem.ViewItem) {
        ItemDetailScreen(
            modifier = modifier,
            onUpClick = { nav.onBackClick() },
            onEditClick = { shareId, itemId, itemType ->
                val destination = when (itemType) {
                    is ItemType.Login -> AppNavItem.EditLogin
                    is ItemType.Note -> AppNavItem.EditNote
                    is ItemType.Alias -> AppNavItem.EditAlias
                    is ItemType.Password -> null // Edit password does not exist yet
                }
                val route = when (itemType) {
                    is ItemType.Login -> AppNavItem.EditLogin.createNavRoute(shareId, itemId)
                    is ItemType.Note -> AppNavItem.EditNote.createNavRoute(shareId, itemId)
                    is ItemType.Alias -> AppNavItem.EditAlias.createNavRoute(shareId, itemId)
                    is ItemType.Password -> null // Edit password does not exist yet
                }

                if (destination != null && route != null) {
                    nav.navigate(destination, route)
                }
            },
            onMovedToTrash = { nav.onBackClick() }
        )
    }
}
