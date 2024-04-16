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

package proton.android.pass.features.security.center.report.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import proton.android.pass.common.api.LoadingResult
import proton.android.pass.common.api.asLoadingResult
import proton.android.pass.common.api.getOrNull
import proton.android.pass.commonui.api.SavedStateHandleProvider
import proton.android.pass.commonui.api.require
import proton.android.pass.commonui.api.toUiModel
import proton.android.pass.crypto.api.context.EncryptionContextProvider
import proton.android.pass.data.api.usecases.ItemTypeFilter
import proton.android.pass.data.api.usecases.ObserveItems
import proton.android.pass.data.api.usecases.breach.ObserveBreachesForAliasEmail
import proton.android.pass.data.api.usecases.breach.ObserveBreachesForCustomEmail
import proton.android.pass.domain.ItemId
import proton.android.pass.domain.ItemState
import proton.android.pass.domain.ItemType
import proton.android.pass.domain.ShareId
import proton.android.pass.domain.ShareSelection
import proton.android.pass.domain.breach.BreachCustomEmailId
import proton.android.pass.features.security.center.report.navigation.BreachCountIdArgId
import proton.android.pass.features.security.center.shared.navigation.BreachEmailIdArgId
import proton.android.pass.features.security.center.shared.navigation.EmailArgId
import proton.android.pass.features.security.center.shared.presentation.AliasEmailType
import proton.android.pass.features.security.center.shared.presentation.CustomEmailType
import proton.android.pass.features.security.center.shared.presentation.EmailType
import proton.android.pass.navigation.api.CommonNavArgId
import proton.android.pass.navigation.api.NavParamEncoder
import proton.android.pass.preferences.UserPreferencesRepository
import proton.android.pass.preferences.value
import javax.inject.Inject

@HiltViewModel
class SecurityCenterReportViewModel @Inject constructor(
    observeBreachesForCustomEmail: ObserveBreachesForCustomEmail,
    observeBreachesForAliasEmail: ObserveBreachesForAliasEmail,
    observeItems: ObserveItems,
    userPreferencesRepository: UserPreferencesRepository,
    encryptionContextProvider: EncryptionContextProvider,
    savedStateHandleProvider: SavedStateHandleProvider
) : ViewModel() {

    private val customEmailId: BreachCustomEmailId? = savedStateHandleProvider.get()
        .get<String>(BreachEmailIdArgId.key)
        ?.let { BreachCustomEmailId(it) }

    private val shareId: ShareId? = savedStateHandleProvider.get()
        .get<String>(CommonNavArgId.ShareId.key)
        ?.let { ShareId(it) }
    private val itemId: ItemId? = savedStateHandleProvider.get()
        .get<String>(CommonNavArgId.ItemId.key)
        ?.let { ItemId(it) }

    private val email: String = savedStateHandleProvider.get()
        .require<String>(EmailArgId.key)
        .let(NavParamEncoder::decode)

    private val breachCount: Int = savedStateHandleProvider.get()
        .require(BreachCountIdArgId.key)

    private val emailType: EmailType by lazy {
        when {
            customEmailId != null -> CustomEmailType(customEmailId)
            shareId != null && itemId != null -> AliasEmailType(shareId, itemId)
            else -> throw IllegalStateException("Invalid state")
        }
    }

    private val breachFlow = when {
        customEmailId != null -> observeBreachesForCustomEmail(id = customEmailId)
        shareId != null && itemId != null -> observeBreachesForAliasEmail(
            shareId = shareId,
            itemId = itemId
        )

        else -> emptyFlow()
    }.asLoadingResult().distinctUntilChanged()
    private val usedInLoginItemsFlow = observeItems(
        selection = ShareSelection.AllShares,
        itemState = ItemState.Active,
        filter = ItemTypeFilter.Logins
    )
        .map { items ->
            items.mapNotNull { item ->
                item.takeIf { (item.itemType as ItemType.Login).username == email }
            }.let {
                encryptionContextProvider.withEncryptionContext {
                    it.map { item -> item.toUiModel(this) }
                }
            }
        }
        .asLoadingResult()
        .distinctUntilChanged()

    internal val state: StateFlow<SecurityCenterReportState> = combine(
        breachFlow,
        usedInLoginItemsFlow,
        userPreferencesRepository.getUseFaviconsPreference()
    ) { breachesForEmailResult, usedInLoginItemsResult, useFavIconsPreference ->
        val isBreachesLoading = breachesForEmailResult is LoadingResult.Loading
        val isUsedInLoading = usedInLoginItemsResult is LoadingResult.Loading
        val breaches = breachesForEmailResult.getOrNull() ?: persistentListOf()
        SecurityCenterReportState(
            breachCount = breachCount,
            usedInItems = usedInLoginItemsResult.getOrNull()
                ?.toImmutableList()
                ?: persistentListOf(),
            email = email,
            canLoadExternalImages = useFavIconsPreference.value(),
            breachEmails = breaches,
            isLoading = isBreachesLoading || isUsedInLoading,
            emailType = emailType
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SecurityCenterReportState.default(email, emailType, breachCount)
    )

}
