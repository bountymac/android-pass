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

package proton.android.pass.features.sharing.sharefromitem

internal sealed interface ShareFromItemEvent {

    data object ShareVault : ShareFromItemEvent

    data object MoveToSharedVault : ShareFromItemEvent

    data object CreateNewVault : ShareFromItemEvent

    data object Upgrade : ShareFromItemEvent

    data object ShareSecureLink : ShareFromItemEvent

    data object UpsellSecureLink : ShareFromItemEvent

    data object ManageSharedVault : ShareFromItemEvent

    data object ShareItem : ShareFromItemEvent

    data object ManageSharedItem : ShareFromItemEvent

    data object UpsellItemSharing : ShareFromItemEvent

}
