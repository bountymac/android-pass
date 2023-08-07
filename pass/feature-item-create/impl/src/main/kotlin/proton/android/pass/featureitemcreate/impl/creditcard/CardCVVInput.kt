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

package proton.android.pass.featureitemcreate.impl.creditcard

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import proton.android.pass.commonui.api.PassTheme
import proton.android.pass.featureitemcreate.impl.R
import proton.android.pass.featureitemcreate.impl.common.ThemedHiddenStatePreviewProvider
import proton.android.pass.featureitemcreate.impl.common.UIHiddenState
import proton.android.pass.composecomponents.impl.R as CompR

@Composable
internal fun CardCVVInput(
    modifier: Modifier = Modifier,
    value: UIHiddenState,
    enabled: Boolean,
    onChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    HiddenNumberInput(
        modifier = modifier,
        value = value,
        enabled = enabled,
        label = stringResource(id = R.string.field_card_cvv_title),
        placeholder = stringResource(id = R.string.field_card_cvv_hint),
        icon = CompR.drawable.ic_verified,
        onChange = onChange,
        onFocusChange = onFocusChange
    )
}

@Preview
@Composable
fun CardCVVInputPreview(
    @PreviewParameter(ThemedHiddenStatePreviewProvider::class) input: Pair<Boolean, UIHiddenState>
) {
    PassTheme(isDark = input.first) {
        Surface {
            CardCVVInput(
                value = input.second,
                enabled = true,
                onChange = {},
                onFocusChange = {}
            )
        }
    }
}
