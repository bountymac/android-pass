/*
 * Copyright (c) 2023-2025 Proton AG
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

package proton.android.pass.features.itemcreate.alias.suffixes.presentation

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import proton.android.pass.common.api.None
import proton.android.pass.common.api.Option
import proton.android.pass.domain.AliasSuffix

@Stable
internal data class SelectSuffixUiState(
    val suffixes: PersistentSet<AliasSuffix>,
    val selectedSuffix: Option<AliasSuffix>,
    val shouldDisplayFeatureDiscoveryBanner: Boolean,
    val canUpgrade: Boolean,
    val event: SelectSuffixEvent
) {

    internal companion object {

        internal val Initial = SelectSuffixUiState(
            suffixes = persistentSetOf(),
            selectedSuffix = None,
            shouldDisplayFeatureDiscoveryBanner = false,
            canUpgrade = false,
            event = SelectSuffixEvent.Idle
        )

    }

}
