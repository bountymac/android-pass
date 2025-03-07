/*
 * Copyright (c) 2023 Proton AG
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

package proton.android.pass.features.sharing.sharingwith

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import proton.android.pass.features.sharing.SharingNavigation

@Composable
fun SharingWithScreen(
    modifier: Modifier = Modifier,
    onNavigateEvent: (SharingNavigation) -> Unit,
    viewModel: SharingWithViewModel = hiltViewModel()
) = with(viewModel) {
    val state by stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(state.event) {
        when (val event = state.event) {
            SharingWithEvents.Idle -> Unit

            is SharingWithEvents.NavigateToPermissions -> SharingNavigation.Permissions(
                shareId = event.shareId,
                itemIdOption = event.itemIdOption
            ).also(onNavigateEvent)
        }

        onConsumeEvent(state.event)
    }

    SharingWithContent(
        modifier = modifier,
        state = state,
        editingEmail = viewModel.editingEmail,
        onEvent = { uiEvent ->
            when (uiEvent) {
                SharingWithUiEvent.ContinueClick -> onContinueClick()
                is SharingWithUiEvent.EmailChange -> onEmailChange(uiEvent.content)
                is SharingWithUiEvent.EmailClick -> onEmailClick(uiEvent.index)
                SharingWithUiEvent.EmailSubmit -> onEmailSubmit()
                is SharingWithUiEvent.InviteSuggestionToggle -> onItemToggle(
                    email = uiEvent.email,
                    checked = uiEvent.value
                )

                SharingWithUiEvent.OnScrolledToBottom -> onScrolledToBottom()
                SharingWithUiEvent.OnBackClick -> onNavigateEvent(SharingNavigation.CloseScreen)
                is SharingWithUiEvent.OnEditVaultClick -> SharingNavigation.EditVault(
                    shareId = uiEvent.shareId
                ).also(onNavigateEvent)
            }
        }
    )
}
