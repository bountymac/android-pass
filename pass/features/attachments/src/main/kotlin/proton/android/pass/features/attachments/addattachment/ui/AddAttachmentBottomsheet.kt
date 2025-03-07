/*
 * Copyright (c) 2024 Proton AG
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

package proton.android.pass.features.attachments.addattachment.ui

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import proton.android.pass.commonui.api.PassTheme
import proton.android.pass.commonui.api.ThemePreviewProvider
import proton.android.pass.commonui.api.bottomSheet
import proton.android.pass.features.attachments.addattachment.navigation.AddAttachmentNavigation

@Composable
fun AddAttachmentBottomsheet(modifier: Modifier = Modifier, onNavigate: (AddAttachmentNavigation) -> Unit) {
    AddAttachmentContent(
        modifier = modifier.bottomSheet(),
        onEvent = {
            when (it) {
                AddAttachmentEvent.ChooseAFile ->
                    onNavigate(AddAttachmentNavigation.OpenFilePicker)
                AddAttachmentEvent.ChooseAPhotoOrVideo ->
                    onNavigate(AddAttachmentNavigation.OpenMediaPicker)
                AddAttachmentEvent.TakeAPhoto ->
                    onNavigate(AddAttachmentNavigation.OpenCamera)
            }
        }
    )
}


@Preview
@Composable
fun AttachmentOptionsBottomsheetPreview(@PreviewParameter(ThemePreviewProvider::class) isDark: Boolean) {
    PassTheme(isDark = isDark) {
        Surface {
            AddAttachmentBottomsheet(
                onNavigate = {}
            )
        }
    }
}
