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

package proton.android.pass.features.home.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import proton.android.pass.commonui.api.PassTheme
import proton.android.pass.commonui.api.Spacing
import proton.android.pass.commonui.api.ThemePreviewProvider
import proton.android.pass.composecomponents.impl.dialogs.ConfirmWithLoadingDialog
import proton.android.pass.composecomponents.impl.text.Text
import proton.android.pass.features.home.R
import me.proton.core.presentation.R as CoreR

@Composable
internal fun ConfirmTrashItemsDialog(
    show: Boolean,
    isLoading: Boolean,
    amount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ConfirmWithLoadingDialog(
        show = show,
        isLoading = isLoading,
        isConfirmActionDestructive = false,
        title = pluralStringResource(R.plurals.alert_confirm_trash_items_title, amount, amount),
        confirmText = stringResource(id = CoreR.string.presentation_alert_ok),
        cancelText = stringResource(id = CoreR.string.presentation_alert_cancel),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onCancel = onDismiss,
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(space = Spacing.medium)
            ) {
                Text.Body1Regular(
                    text = pluralStringResource(
                        R.plurals.alert_confirm_trash_items_message,
                        amount,
                        amount
                    )
                )
            }
        }
    )
}

@[Preview Composable]
internal fun ConfirmTrashItemsDialogPreview(@PreviewParameter(ThemePreviewProvider::class) isDark: Boolean) {
    PassTheme(isDark = isDark) {
        Surface {
            ConfirmTrashItemsDialog(
                show = true,
                isLoading = false,
                amount = 1,
                onDismiss = {},
                onConfirm = {}
            )
        }
    }
}
