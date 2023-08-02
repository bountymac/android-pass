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

package proton.android.pass.featuresharing.impl.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import proton.android.pass.commonui.api.PassTheme
import proton.android.pass.commonui.api.ThemedBooleanPreviewProvider
import proton.android.pass.commonui.api.body3Norm
import proton.android.pass.commonui.api.body3Weak
import proton.android.pass.composecomponents.impl.container.CircleTextIcon
import proton.android.pass.data.api.usecases.VaultMember
import proton.android.pass.featuresharing.impl.R
import proton.android.pass.featuresharing.impl.common.toShortSummary
import proton.pass.domain.InviteId
import proton.pass.domain.ShareId
import proton.pass.domain.ShareRole
import proton.android.pass.composecomponents.impl.R as CompR

@Composable
fun ManageVaultMemberRow(
    modifier: Modifier = Modifier,
    member: VaultMember,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircleTextIcon(
            text = member.email,
            backgroundColor = PassTheme.colors.interactionNormMinor1,
            textColor = PassTheme.colors.interactionNormMajor1,
            shape = PassTheme.shapes.squircleMediumShape
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = member.email,
                style = PassTheme.typography.body3Norm()
            )

            when (member) {
                is VaultMember.Member -> {
                    member.role?.let { role ->
                        Text(
                            text = role.toShortSummary(),
                            style = PassTheme.typography.body3Weak()
                        )
                    }

                }

                is VaultMember.InvitePending -> {
                    Text(
                        text = stringResource(R.string.share_manage_vault_invite_pending),
                        style = PassTheme.typography.body3Weak()
                    )
                }
            }
        }

        IconButton(
            onClick = { onOptionsClick() },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(CompR.drawable.ic_three_dots_vertical_24),
                contentDescription = stringResource(id = CompR.string.action_content_description_menu),
                tint = PassTheme.colors.textHint
            )
        }
    }
}

@Preview
@Composable
fun ManageVaultMemberRowPreview(
    @PreviewParameter(ThemedBooleanPreviewProvider::class) input: Pair<Boolean, Boolean>
) {
    val member = if (input.second) {
        VaultMember.Member(
            email = "some@email.test",
            shareId = ShareId("123"),
            username = "some username",
            role = ShareRole.Admin
        )
    } else {
        VaultMember.InvitePending(email = "invited@email.test", inviteId = InviteId("123"))
    }
    PassTheme(isDark = input.first) {
        Surface {
            ManageVaultMemberRow(
                member = member,
                onOptionsClick = {}
            )
        }
    }
}
