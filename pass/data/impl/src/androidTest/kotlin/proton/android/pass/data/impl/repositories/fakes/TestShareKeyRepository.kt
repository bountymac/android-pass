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

package proton.android.pass.data.impl.repositories.fakes

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import proton.android.pass.common.api.FlowUtils.testFlow
import proton.android.pass.data.impl.db.entities.ShareKeyEntity
import proton.android.pass.data.impl.repositories.ShareKeyRepository
import proton.android.pass.domain.ShareId
import proton.android.pass.domain.key.ShareKey

class TestShareKeyRepository : ShareKeyRepository {

    private var getShareKeysFlow = testFlow<List<ShareKey>>()
    private var getLatestKeyForShareFlow = testFlow<ShareKey>()
    private var getShareKeyForRotationFlow = testFlow<ShareKey?>()

    fun emitGetShareKeys(value: List<ShareKey>) {
        getShareKeysFlow.tryEmit(value)
    }

    fun emitGetLatestKeyForShare(value: ShareKey) {
        getLatestKeyForShareFlow.tryEmit(value)
    }

    fun emitGetShareKeyForRotation(value: ShareKey?) {
        getShareKeyForRotationFlow.tryEmit(value)
    }

    override fun getShareKeys(
        userId: UserId,
        addressId: AddressId,
        shareId: ShareId,
        forceRefresh: Boolean,
        shouldStoreLocally: Boolean
    ): Flow<List<ShareKey>> = getShareKeysFlow

    override fun getLatestKeyForShare(shareId: ShareId): Flow<ShareKey> = getLatestKeyForShareFlow

    override fun getShareKeyForRotation(
        userId: UserId,
        addressId: AddressId,
        shareId: ShareId,
        keyRotation: Long
    ): Flow<ShareKey?> = getShareKeyForRotationFlow

    override suspend fun saveShareKeys(shareKeyEntities: List<ShareKeyEntity>) {

    }
}
