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

package proton.android.pass.features.extrapassword.configure.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import proton.android.pass.features.extrapassword.ExtraPasswordNavigation
import proton.android.pass.features.extrapassword.configure.navigation.SetExtraPasswordContentNavEvent
import proton.android.pass.features.extrapassword.configure.presentation.SetExtraPasswordEvent
import proton.android.pass.features.extrapassword.configure.presentation.SetExtraPasswordViewModel

@Composable
fun SetExtraPasswordScreen(
    modifier: Modifier = Modifier,
    viewModel: SetExtraPasswordViewModel = hiltViewModel(),
    onNavigate: (ExtraPasswordNavigation) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.event) {
        when (val event = state.event) {
            SetExtraPasswordEvent.Idle -> {}
            is SetExtraPasswordEvent.Success -> onNavigate(ExtraPasswordNavigation.Confirm(event.password))
        }
        viewModel.onEventConsumed(state.event)
    }

    SetExtraPasswordContent(
        modifier = modifier,
        formState = viewModel.getExtraPasswordState(),
        validationError = state.validationError,
        onEvent = {
            when (it) {
                is SetExtraPasswordContentNavEvent.Back -> onNavigate(ExtraPasswordNavigation.CloseScreen)
                is SetExtraPasswordContentNavEvent.OnExtraPasswordRepeatValueChangedNav ->
                    viewModel.onExtraPasswordRepeatValueChanged(it.value)

                is SetExtraPasswordContentNavEvent.OnExtraPasswordValueChangedNav ->
                    viewModel.onExtraPasswordValueChanged(it.value)

                SetExtraPasswordContentNavEvent.Submit -> {
                    viewModel.submit()
                }
            }
        }
    )
}

