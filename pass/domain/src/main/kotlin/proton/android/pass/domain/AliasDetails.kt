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

package proton.android.pass.domain

data class AliasDetails(
    val email: String,
    val mailboxes: List<AliasMailbox>,
    val availableMailboxes: List<AliasMailbox>,
    val stats: AliasStats
) {
    companion object {
        val EMPTY = AliasDetails(
            email = "",
            mailboxes = emptyList(),
            availableMailboxes = emptyList(),
            stats = AliasStats(0, 0, 0)
        )
    }
}

data class AliasStats(
    val forwardedEmails: Int,
    val repliedEmails: Int,
    val blockedEmails: Int
)
