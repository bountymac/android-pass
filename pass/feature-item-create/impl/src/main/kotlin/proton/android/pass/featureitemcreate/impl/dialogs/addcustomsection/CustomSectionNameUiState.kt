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

import javax.annotation.concurrent.Immutable

sealed interface CustomSectionEvent {
    data object Close : CustomSectionEvent
    data object Idle : CustomSectionEvent
}

@Immutable
data class CustomSectionNameUiState(
    val value: String,
    val canConfirm: Boolean,
    val event: CustomSectionEvent
) {
    companion object {
        val Initial = CustomSectionNameUiState(
            value = "",
            canConfirm = false,
            event = CustomSectionEvent.Idle
        )
    }
}
