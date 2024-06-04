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

package proton.android.pass.featureitemcreate.impl.dialogs.addcustomsection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import proton.android.pass.composecomponents.impl.dialogs.NoPaddingDialog
import proton.android.pass.featureitemcreate.impl.R
import proton.android.pass.featureitemcreate.impl.dialogs.SingleInputDialogContent
import proton.android.pass.featureitemcreate.impl.dialogs.addcustomfield.ExtraFieldNameNavigation

@Composable
fun CustomSectionNameDialog(
    modifier: Modifier = Modifier,
    onNavigate: (ExtraFieldNameNavigation) -> Unit,
    viewModel: CustomSectionNameViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.event) {
        when (state.event) {
            CustomSectionEvent.Close -> onNavigate(ExtraFieldNameNavigation.Close)
            CustomSectionEvent.Idle -> {}
        }
        viewModel.consumeEvent(state.event)
    }

    NoPaddingDialog(
        modifier = modifier,
        onDismissRequest = { onNavigate(ExtraFieldNameNavigation.Close) }
    ) {
        SingleInputDialogContent(
            value = state.value,
            canConfirm = state.canConfirm,
            titleRes = R.string.custom_section_dialog_title,
            placeholderRes = R.string.custom_section_dialog_placeholder,
            onChange = viewModel::onNameChanged,
            onConfirm = viewModel::onSave,
            onCancel = { onNavigate(ExtraFieldNameNavigation.Close) }
        )
    }
}
