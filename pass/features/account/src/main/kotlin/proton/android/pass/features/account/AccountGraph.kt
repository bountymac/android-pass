/*
 * Copyright (c) 2023-2024 Proton AG
 * This file is part of Proton AG and Proton Pass.
 *
 * Proton Pass is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Pass is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Pass.  If not, see <https://www.gnu.org/licenses/>.
 */

package proton.android.pass.features.account

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import proton.android.pass.navigation.api.NavItem
import proton.android.pass.navigation.api.composable

private const val ACCOUNT_GRAPH = "account_graph"

object Account : NavItem(baseRoute = "account/view")

fun NavGraphBuilder.accountGraph(onNavigate: (AccountNavigation) -> Unit, subGraph: NavGraphBuilder.() -> Unit = {}) {
    navigation(
        route = ACCOUNT_GRAPH,
        startDestination = Account.route
    ) {
        composable(Account) {
            AccountScreen(
                modifier = Modifier.testTag(AccountScreenTestTag.SCREEN),
                onNavigate = onNavigate
            )
        }
        subGraph()
    }
}

