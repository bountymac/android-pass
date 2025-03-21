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

package proton.android.pass.features.item.history.restore.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import proton.android.pass.common.api.FlowUtils.oneShot
import proton.android.pass.common.api.None
import proton.android.pass.common.api.Option
import proton.android.pass.common.api.Some
import proton.android.pass.common.api.some
import proton.android.pass.commonpresentation.api.items.details.domain.ItemDetailsFieldType
import proton.android.pass.commonpresentation.api.items.details.handlers.ItemDetailsHandler
import proton.android.pass.commonpresentation.api.items.details.handlers.ItemDetailsSource
import proton.android.pass.commonui.api.SavedStateHandleProvider
import proton.android.pass.commonui.api.require
import proton.android.pass.commonui.api.toItemContents
import proton.android.pass.commonuimodels.api.items.ItemDetailState
import proton.android.pass.crypto.api.context.EncryptionContextProvider
import proton.android.pass.data.api.repositories.ItemRevision
import proton.android.pass.data.api.usecases.GetItemById
import proton.android.pass.data.api.usecases.attachments.LinkAttachmentsToItem
import proton.android.pass.data.api.usecases.attachments.RestoreAttachments
import proton.android.pass.data.api.usecases.attachments.SetAttachmentToBeUnlinked
import proton.android.pass.data.api.usecases.items.OpenItemRevision
import proton.android.pass.data.api.usecases.items.RestoreItemRevision
import proton.android.pass.domain.HiddenState
import proton.android.pass.domain.ItemContents
import proton.android.pass.domain.ItemCustomFieldSection
import proton.android.pass.domain.ItemDiffs
import proton.android.pass.domain.ItemId
import proton.android.pass.domain.ShareId
import proton.android.pass.domain.attachments.Attachment
import proton.android.pass.domain.attachments.AttachmentId
import proton.android.pass.features.item.history.navigation.ItemHistoryRevisionNavArgId
import proton.android.pass.log.api.PassLogger
import proton.android.pass.navigation.api.CommonNavArgId
import proton.android.pass.navigation.api.NavParamEncoder
import proton.android.pass.notifications.api.SnackbarDispatcher
import proton.android.pass.preferences.FeatureFlag
import proton.android.pass.preferences.FeatureFlagsPreferencesRepository
import javax.inject.Inject

private const val TAG = "ItemHistoryRestoreViewModel"

@HiltViewModel
class ItemHistoryRestoreViewModel @Inject constructor(
    savedStateHandleProvider: SavedStateHandleProvider,
    openItemRevision: OpenItemRevision,
    featureFlagsRepository: FeatureFlagsPreferencesRepository,
    private val restoreItemRevision: RestoreItemRevision,
    private val restoreAttachments: RestoreAttachments,
    private val linkAttachmentsToItem: LinkAttachmentsToItem,
    private val setAttachmentToBeUnlinked: SetAttachmentToBeUnlinked,
    private val itemDetailsHandler: ItemDetailsHandler,
    private val snackbarDispatcher: SnackbarDispatcher,
    private val encryptionContextProvider: EncryptionContextProvider,
    private val getItemById: GetItemById
) : ViewModel() {

    private val shareId: ShareId = savedStateHandleProvider.get()
        .require<String>(CommonNavArgId.ShareId.key)
        .let(::ShareId)

    private val itemId: ItemId = savedStateHandleProvider.get()
        .require<String>(CommonNavArgId.ItemId.key)
        .let(::ItemId)

    private val itemRevision: ItemRevision = savedStateHandleProvider.get()
        .require<String>(ItemHistoryRevisionNavArgId.key)
        .let { encodedRevision -> Json.decodeFromString(NavParamEncoder.decode(encodedRevision)) }

    private val revisionItemFlow = oneShot {
        encryptionContextProvider.withEncryptionContextSuspendable {
            openItemRevision(shareId, itemRevision, this@withEncryptionContextSuspendable)
        }
    }

    private val revisionItemContentsFlow = revisionItemFlow.map { revisionItem ->
        encryptionContextProvider.withEncryptionContext {
            toItemContents(
                itemType = revisionItem.itemType,
                encryptionContext = this,
                title = revisionItem.title,
                note = revisionItem.note,
                flags = revisionItem.flags
            )
        }
    }

    private val currentItemFlow = oneShot {
        getItemById(shareId, itemId)
    }

    private val currentItemContentsFlow = currentItemFlow.map { currentItem ->
        encryptionContextProvider.withEncryptionContext {
            toItemContents(
                itemType = currentItem.itemType,
                encryptionContext = this,
                title = currentItem.title,
                note = currentItem.note,
                flags = currentItem.flags
            )
        }
    }

    private val revisionItemDiffsFlow = combine(
        revisionItemFlow.map { revisionItem -> revisionItem.itemType.category },
        revisionItemContentsFlow,
        currentItemContentsFlow,
        flowOf(emptyList<Attachment>()),
        flowOf(emptyList<Attachment>())
    ) { itemCategory, baseItemContents, otherItemContents, baseAttachments, otherAttachments ->
        itemDetailsHandler.updateItemDetailsDiffs(
            itemCategory = itemCategory,
            baseItemContents = baseItemContents,
            otherItemContents = otherItemContents,
            baseAttachments = baseAttachments,
            otherAttachments = otherAttachments
        )
    }

    private val revisionItemContentsUpdateOptionFlow = MutableStateFlow<Option<ItemContents>>(None)

    private val revisionItemDetailsStateFlow = revisionItemFlow.flatMapLatest { item ->
        createItemDetailsStateFlow(
            itemContentsUpdateOptionFlow = revisionItemContentsUpdateOptionFlow,
            itemDetailStateFlow = itemDetailsHandler.observeItemDetails(
                item = item,
                source = ItemDetailsSource.REVISION
            ),
            itemDiffsFlow = revisionItemDiffsFlow
        )
    }

    private val currentItemDiffsFlow = combine(
        currentItemFlow.map { currentItem -> currentItem.itemType.category },
        currentItemContentsFlow,
        revisionItemContentsFlow,
        flowOf(emptyList<Attachment>()),
        flowOf(emptyList<Attachment>())
    ) { itemCategory, baseItemContents, otherItemContents, baseAttachments, otherAttachments ->
        itemDetailsHandler.updateItemDetailsDiffs(
            itemCategory = itemCategory,
            baseItemContents = baseItemContents,
            otherItemContents = otherItemContents,
            baseAttachments = baseAttachments,
            otherAttachments = otherAttachments
        )
    }

    private val currentItemContentsUpdateOptionFlow = MutableStateFlow<Option<ItemContents>>(None)

    private val currentItemDetailsStateFlow = currentItemFlow.flatMapLatest { item ->
        createItemDetailsStateFlow(
            itemContentsUpdateOptionFlow = currentItemContentsUpdateOptionFlow,
            itemDetailStateFlow = itemDetailsHandler.observeItemDetails(
                item = item,
                source = ItemDetailsSource.REVISION
            ),
            itemDiffsFlow = currentItemDiffsFlow
        )
    }

    private val eventFlow = MutableStateFlow<ItemHistoryRestoreEvent>(ItemHistoryRestoreEvent.Idle)

    internal val state = combine(
        currentItemDetailsStateFlow,
        revisionItemDetailsStateFlow,
        eventFlow,
        featureFlagsRepository.get<Boolean>(FeatureFlag.FILE_ATTACHMENTS_V1)
    ) { currentItemDetailState, revisionItemDetailState, event, isFileAttachmentEnabled ->
        ItemHistoryRestoreState.ItemDetails(
            itemRevision = itemRevision,
            currentItemDetailState = currentItemDetailState,
            revisionItemDetailState = revisionItemDetailState,
            event = event,
            isFileAttachmentEnabled = isFileAttachmentEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ItemHistoryRestoreState.Initial
    )

    private fun createItemDetailsStateFlow(
        itemContentsUpdateOptionFlow: Flow<Option<ItemContents>>,
        itemDetailStateFlow: Flow<ItemDetailState>,
        itemDiffsFlow: Flow<ItemDiffs>
    ) = combine(
        itemContentsUpdateOptionFlow,
        itemDetailStateFlow,
        itemDiffsFlow
    ) { itemContentsUpdateOption, itemDetailState, itemDiffs ->
        when (itemContentsUpdateOption) {
            None -> itemDetailState.itemContents
            is Some -> itemContentsUpdateOption.value
        }.let { itemContents ->
            itemDetailState.update(itemContents = itemContents, itemDiffs = itemDiffs)
        }
    }

    internal fun onEventConsumed(event: ItemHistoryRestoreEvent) {
        eventFlow.compareAndSet(event, ItemHistoryRestoreEvent.Idle)
    }

    internal fun onItemFieldClicked(text: String, plainFieldType: ItemDetailsFieldType.Plain) {
        viewModelScope.launch {
            itemDetailsHandler.onItemDetailsFieldClicked(text, plainFieldType)
        }
    }

    internal fun onItemHiddenFieldClicked(hiddenState: HiddenState, hiddenFieldType: ItemDetailsFieldType.Hidden) {
        viewModelScope.launch {
            itemDetailsHandler.onItemDetailsHiddenFieldClicked(hiddenState, hiddenFieldType)
        }
    }

    internal fun onToggleItemHiddenField(
        selection: ItemHistoryRestoreSelection,
        isVisible: Boolean,
        hiddenState: HiddenState,
        hiddenFieldType: ItemDetailsFieldType.Hidden,
        hiddenFieldSection: ItemCustomFieldSection
    ) {
        when (val stateValue = state.value) {
            ItemHistoryRestoreState.Initial -> return
            is ItemHistoryRestoreState.ItemDetails -> {
                when (selection) {
                    ItemHistoryRestoreSelection.Revision -> {
                        itemDetailsHandler.updateItemDetailsContent(
                            isVisible = isVisible,
                            hiddenState = hiddenState,
                            hiddenFieldType = hiddenFieldType,
                            hiddenFieldSection = hiddenFieldSection,
                            itemCategory = stateValue.revisionItemDetailState.itemCategory,
                            itemContents = stateValue.revisionItemDetailState.itemContents
                        ).also { updatedItemContents ->
                            revisionItemContentsUpdateOptionFlow.update { updatedItemContents.some() }
                        }
                    }

                    ItemHistoryRestoreSelection.Current -> {
                        itemDetailsHandler.updateItemDetailsContent(
                            isVisible = isVisible,
                            hiddenState = hiddenState,
                            hiddenFieldType = hiddenFieldType,
                            hiddenFieldSection = hiddenFieldSection,
                            itemCategory = stateValue.currentItemDetailState.itemCategory,
                            itemContents = stateValue.currentItemDetailState.itemContents
                        ).also { updatedItemContents ->
                            currentItemContentsUpdateOptionFlow.update { updatedItemContents.some() }
                        }
                    }
                }
            }
        }
    }

    internal fun onRestoreItem() {
        eventFlow.update { ItemHistoryRestoreEvent.OnRestoreItem }
    }

    internal fun onRestoreItemCanceled() {
        eventFlow.update { ItemHistoryRestoreEvent.OnRestoreItemCanceled }
    }

    internal fun onRestoreItemConfirmed(
        itemContents: ItemContents,
        attachmentsToRestore: Set<AttachmentId>,
        attachmentsToDelete: Set<AttachmentId>
    ) {
        viewModelScope.launch {
            eventFlow.update { ItemHistoryRestoreEvent.OnRestoreItemConfirmed }

            runCatching { restoreItemRevision(shareId, itemId, itemContents) }
                .onSuccess { revision ->
                    if (attachmentsToRestore.isNotEmpty()) {
                        runCatching {
                            restoreAttachments(shareId, itemId, attachmentsToRestore)
                        }.onFailure {
                            PassLogger.w(TAG, "Error restoring attachments")
                            PassLogger.w(TAG, it)
                        }
                    }

                    if (attachmentsToDelete.isNotEmpty()) {
                        runCatching {
                            setAttachmentToBeUnlinked(attachmentsToDelete)
                            linkAttachmentsToItem(shareId, itemId, revision)
                        }.onFailure {
                            PassLogger.w(TAG, "Error unlinking attachments")
                            PassLogger.w(TAG, it)
                        }
                    }
                    eventFlow.update { ItemHistoryRestoreEvent.OnItemRestored }
                    snackbarDispatcher(ItemHistoryRestoreSnackbarMessage.RestoreItemRevisionSuccess)
                }
                .onFailure { error ->
                    PassLogger.w(TAG, "Error restoring item revision: $error")
                    eventFlow.update { ItemHistoryRestoreEvent.OnRestoreItemCanceled }
                    snackbarDispatcher(ItemHistoryRestoreSnackbarMessage.RestoreItemRevisionError)
                }
        }
    }

}
