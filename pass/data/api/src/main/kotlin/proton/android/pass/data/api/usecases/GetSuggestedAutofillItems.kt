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

package proton.android.pass.data.api.usecases

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import proton.android.pass.common.api.None
import proton.android.pass.common.api.Option
import proton.android.pass.domain.Item

interface GetSuggestedAutofillItems {
    operator fun invoke(
        itemTypeFilter: ItemTypeFilter,
        suggestion: Suggestion,
        userId: Option<UserId> = None
    ): Flow<SuggestedAutofillItemsResult>
}

sealed interface SuggestedAutofillItemsResult {
    @JvmInline
    value class Items(val suggestedItems: List<ItemData.SuggestedItem>) :
        SuggestedAutofillItemsResult
    data object ShowUpgrade : SuggestedAutofillItemsResult
}

sealed interface ItemData {

    val item: Item

    @JvmInline
    value class DefaultItem(override val item: Item) : ItemData

    data class SuggestedItem(
        override val item: Item,
        val suggestion: Suggestion
    ) : ItemData {
        val isDALSuggestion: Boolean = (suggestion as? Suggestion.Url)?.isDALSuggestion ?: false
    }
}

sealed interface Suggestion {
    val value: String

    @JvmInline
    value class PackageName(override val value: String) : Suggestion

    data class Url(
        override val value: String,
        val isDALSuggestion: Boolean = false
    ) : Suggestion
}
