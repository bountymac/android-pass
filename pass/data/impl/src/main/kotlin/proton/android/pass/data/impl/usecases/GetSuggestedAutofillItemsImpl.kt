package proton.android.pass.data.impl.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getAccounts
import proton.android.pass.common.api.Option
import proton.android.pass.common.api.toOption
import proton.android.pass.data.api.usecases.GetSuggestedAutofillItems
import proton.android.pass.data.api.usecases.GetUserPlan
import proton.android.pass.data.api.usecases.ItemTypeFilter
import proton.android.pass.data.api.usecases.ObserveItems
import proton.android.pass.data.api.usecases.ObserveUsableVaults
import proton.android.pass.data.api.usecases.SuggestedAutofillItemsResult
import proton.android.pass.data.impl.autofill.SuggestionItemFilterer
import proton.android.pass.data.impl.autofill.SuggestionSorter
import proton.android.pass.domain.Item
import proton.android.pass.domain.ItemState
import proton.android.pass.domain.PlanType
import proton.android.pass.preferences.InternalSettingsRepository
import javax.inject.Inject

class GetSuggestedAutofillItemsImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val observeItems: ObserveItems,
    private val suggestionItemFilter: SuggestionItemFilterer,
    private val suggestionSorter: SuggestionSorter,
    private val observeUsableVaults: ObserveUsableVaults,
    private val getUserPlan: GetUserPlan,
    private val internalSettingsRepository: InternalSettingsRepository
) : GetSuggestedAutofillItems {

    override fun invoke(
        itemTypeFilter: ItemTypeFilter,
        packageName: Option<String>,
        url: Option<String>
    ): Flow<SuggestedAutofillItemsResult> = accountManager.getAccounts(AccountState.Ready)
        .flatMapLatest { accounts ->
            val accountFlows = accounts.map { account ->
                when (itemTypeFilter) {
                    ItemTypeFilter.Notes,
                    ItemTypeFilter.Aliases,
                    ItemTypeFilter.All -> throw IllegalArgumentException("ItemType is not supported")

                    ItemTypeFilter.Logins,
                    ItemTypeFilter.Identity ->
                        getSuggestedItemsForAccount(
                            account = account,
                            itemTypeFilter = itemTypeFilter,
                            packageName = packageName,
                            url = url
                        ).map { SuggestedAutofillItemsResult.Items(it) }

                    ItemTypeFilter.CreditCards ->
                        combine(
                            getSuggestedItemsForAccount(
                                account = account,
                                itemTypeFilter = itemTypeFilter,
                                packageName = packageName,
                                url = url
                            ),
                            getUserPlan(account.userId)
                        ) { items, plan ->
                            when (plan.planType) {
                                is PlanType.Free -> if (items.isEmpty()) {
                                    SuggestedAutofillItemsResult.Items(emptyList())
                                } else {
                                    SuggestedAutofillItemsResult.ShowUpgrade
                                }

                                is PlanType.Paid,
                                is PlanType.Trial -> SuggestedAutofillItemsResult.Items(items)

                                is PlanType.Unknown ->
                                    SuggestedAutofillItemsResult.Items(emptyList())
                            }
                        }
                }
            }
            combine(accountFlows) { results ->
                if (results.all { it is SuggestedAutofillItemsResult.ShowUpgrade }) {
                    SuggestedAutofillItemsResult.ShowUpgrade
                } else {
                    val combinedItems = results.filterIsInstance<SuggestedAutofillItemsResult.Items>()
                        .flatMap { it.items }
                    SuggestedAutofillItemsResult.Items(combinedItems)
                }
            }
        }

    private fun getSuggestedItemsForAccount(
        account: Account,
        itemTypeFilter: ItemTypeFilter,
        packageName: Option<String>,
        url: Option<String>
    ): Flow<List<Item>> = observeUsableVaults(account.userId).flatMapLatest { usableVaults ->
        observeItems(
            userId = account.userId,
            filter = itemTypeFilter,
            selection = usableVaults,
            itemState = ItemState.Active
        )
            .map { items -> suggestionItemFilter.filter(items, packageName, url) }
            .map { suggestions -> sortSuggestions(suggestions, url) }
    }

    private suspend fun sortSuggestions(items: List<Item>, url: Option<String>): List<Item> = suggestionSorter.sort(
        items = items,
        url = url,
        lastItemAutofill = internalSettingsRepository.getLastItemAutofill()
            .firstOrNull()
            .toOption()
            .flatMap { it }
    )
}
