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

package proton.android.pass.data.impl.usecases

import kotlinx.coroutines.flow.first
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import proton.android.pass.data.api.repositories.ItemRepository
import proton.android.pass.data.api.usecases.GetItemByAliasEmail
import proton.pass.domain.Item
import javax.inject.Inject

class GetItemByAliasEmailImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val itemRepository: ItemRepository
) : GetItemByAliasEmail {
    override suspend fun invoke(userId: UserId?, aliasEmail: String): Item? {
        val id = userId ?: requireNotNull(accountManager.getPrimaryUserId().first())
        return itemRepository.getItemByAliasEmail(id, aliasEmail)
    }
}
