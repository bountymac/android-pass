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

package proton.android.pass.features.trial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import proton.android.pass.commonui.api.PassTheme
import proton.android.pass.commonui.api.Spacing
import proton.android.pass.commonui.api.ThemePreviewProvider
import proton.android.pass.composecomponents.impl.container.RoundedCornersColumn
import proton.android.pass.composecomponents.impl.form.PassDivider
import me.proton.core.presentation.R as CoreR

@Composable
fun TrialFeatures(modifier: Modifier = Modifier) {
    RoundedCornersColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        TrialFeatureRow(
            modifier = Modifier.padding(
                start = Spacing.medium,
                end = Spacing.medium,
                top = Spacing.medium,
                bottom = Spacing.none
            ),
            feature = stringResource(R.string.trial_feature_1),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.trial_multiple_vaults),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
            }
        )
        PassDivider()
        TrialFeatureRow(
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = 8.dp),
            feature = stringResource(R.string.trial_feature_3),
            icon = {
                Icon(
                    modifier = Modifier.padding(start = Spacing.extraSmall), // Needed to match alignment with 1
                    painter = painterResource(CoreR.drawable.ic_proton_lock),
                    contentDescription = null,
                    tint = PassTheme.colors.loginInteractionNorm
                )
            }
        )
        PassDivider()
        TrialFeatureRow(
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = 8.dp),
            feature = stringResource(R.string.trial_feature_4),
            icon = {
                Icon(
                    modifier = Modifier.padding(start = Spacing.extraSmall), // Needed to match alignment with 1
                    painter = painterResource(CoreR.drawable.ic_proton_list_bullets),
                    contentDescription = null,
                    tint = PassTheme.colors.aliasInteractionNormMajor2
                )
            }
        )
        PassDivider()

    }
}

@Preview
@Composable
fun TrialFeaturesPreview(@PreviewParameter(ThemePreviewProvider::class) isDark: Boolean) {
    PassTheme(isDark = isDark) {
        Surface {
            TrialFeatures()
        }
    }
}
