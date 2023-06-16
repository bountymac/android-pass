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

package proton.android.pass.data.impl.local

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import proton.android.pass.data.impl.db.entities.ShareEntity
import proton.pass.domain.ShareId

interface LocalShareDataSource {
    suspend fun upsertShares(shares: List<ShareEntity>)
    suspend fun getById(userId: UserId, shareId: ShareId): ShareEntity?
    fun getAllSharesForUser(userId: UserId): Flow<List<ShareEntity>>
    fun observeAllActiveSharesForUser(userId: UserId): Flow<List<ShareEntity>>
    fun getAllSharesForAddress(addressId: AddressId): Flow<List<ShareEntity>>
    fun observeActiveVaultCount(userId: UserId): Flow<Int>
    suspend fun deleteShares(shareIds: Set<ShareId>): Boolean
    suspend fun hasShares(userId: UserId): Boolean
    suspend fun disablePrimaryShare(userId: UserId)
    suspend fun setPrimaryShareStatus(userId: UserId, shareId: ShareId, isPrimary: Boolean)
    suspend fun deleteSharesForUser(userId: UserId)
}
