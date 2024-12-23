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

package proton.android.pass.featureitemcreate.impl.common.attachments

import android.content.Context
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import proton.android.pass.common.api.None
import proton.android.pass.common.api.Option
import proton.android.pass.common.api.Some
import proton.android.pass.commonui.api.ClassHolder
import proton.android.pass.commonui.api.FileHandler
import proton.android.pass.commonuimodels.api.attachments.AttachmentsState
import proton.android.pass.data.api.repositories.DraftAttachmentRepository
import proton.android.pass.data.api.repositories.MetadataResolver
import proton.android.pass.data.api.usecases.attachments.ClearAttachments
import proton.android.pass.data.api.usecases.attachments.DownloadAttachment
import proton.android.pass.data.api.usecases.attachments.ObserveUpdateItemAttachments
import proton.android.pass.data.api.usecases.attachments.UploadAttachment
import proton.android.pass.domain.ItemId
import proton.android.pass.domain.ShareId
import proton.android.pass.domain.attachments.Attachment
import proton.android.pass.domain.attachments.AttachmentId
import proton.android.pass.featureitemcreate.impl.R
import proton.android.pass.featureitemcreate.impl.common.attachments.AttachmentSnackbarMessages.OpenAttachmentsError
import proton.android.pass.featureitemcreate.impl.common.attachments.AttachmentSnackbarMessages.UploadAttachmentsError
import proton.android.pass.log.api.PassLogger
import proton.android.pass.notifications.api.SnackbarDispatcher
import java.net.URI
import javax.inject.Inject

@ViewModelScoped
class AttachmentsHandlerImpl @Inject constructor(
    private val draftAttachmentRepository: DraftAttachmentRepository,
    private val metadataResolver: MetadataResolver,
    private val uploadAttachment: UploadAttachment,
    private val downloadAttachment: DownloadAttachment,
    private val clearAttachments: ClearAttachments,
    private val observeUpdateItemAttachments: ObserveUpdateItemAttachments,
    private val fileHandler: FileHandler,
    private val snackbarDispatcher: SnackbarDispatcher
) : AttachmentsHandler {

    private val shareIdState = MutableStateFlow<Option<ShareId>>(None)
    private val itemIdState = MutableStateFlow<Option<ItemId>>(None)
    private val loadingDraftAttachments: MutableStateFlow<Set<URI>> =
        MutableStateFlow(emptySet())
    private val loadingAttachments: MutableStateFlow<Set<AttachmentId>> =
        MutableStateFlow(emptySet())
    private val draftAttachments = draftAttachmentRepository.observeAll()
        .map { uris -> uris.mapNotNull { metadataResolver.extractMetadata(it) } }

    private val attachments = combine(
        shareIdState,
        itemIdState,
        ::Pair
    ).flatMapLatest { (shareId, itemId) ->
        if (shareId is Some && itemId is Some) {
            observeUpdateItemAttachments(shareId.value, itemId.value)
        } else {
            flowOf(emptyList())
        }
    }

    override val isUploadingAttachment: StateFlow<Set<URI>> get() = loadingDraftAttachments

    override val attachmentState: Flow<AttachmentsState> = combine(
        draftAttachments,
        attachments,
        loadingDraftAttachments,
        loadingAttachments,
        ::AttachmentsState
    ).distinctUntilChanged()

    override fun openDraftAttachment(
        contextHolder: ClassHolder<Context>,
        uri: URI,
        mimetype: String
    ) {
        fileHandler.openFile(
            contextHolder = contextHolder,
            uri = uri,
            mimeType = mimetype,
            chooserTitle = contextHolder.get().value()?.getString(R.string.open_with) ?: ""
        )
    }

    override suspend fun openAttachment(contextHolder: ClassHolder<Context>, attachment: Attachment) {
        loadingAttachments.update { it + attachment.id }
        runCatching {
            val uri = downloadAttachment(attachment)
            fileHandler.openFile(
                contextHolder = contextHolder,
                uri = uri,
                mimeType = attachment.mimeType,
                chooserTitle = contextHolder.get().value()?.getString(R.string.open_with) ?: ""
            )
        }.onSuccess {
            PassLogger.i(TAG, "Attachment opened: ${attachment.id}")
        }.onFailure {
            PassLogger.w(TAG, "Could not open attachment: ${attachment.id}")
            PassLogger.w(TAG, it)
            snackbarDispatcher(OpenAttachmentsError)
        }
        loadingAttachments.update { it - attachment.id }
    }

    override suspend fun uploadNewAttachment(uri: URI) {
        loadingDraftAttachments.update { it + uri }
        runCatching { uploadAttachment(uri) }
            .onSuccess {
                PassLogger.i(TAG, "Attachment uploaded: $uri")
            }
            .onFailure {
                PassLogger.w(TAG, "Could not upload attachment: $uri")
                PassLogger.w(TAG, it)
                snackbarDispatcher(UploadAttachmentsError)
            }
        loadingDraftAttachments.update { it - uri }
    }

    override fun onClearAttachments() {
        clearAttachments()
    }

    override fun observeNewAttachments(onNewAttachment: (Set<URI>) -> Unit): Flow<Set<URI>> =
        draftAttachmentRepository.observeNew()
            .onEach { newUris ->
                if (newUris.isNotEmpty()) onNewAttachment(newUris)
            }

    override suspend fun getAttachmentsForItem(shareId: ShareId, itemId: ItemId) {
        shareIdState.update { Some(shareId) }
        itemIdState.update { Some(itemId) }
    }

    companion object {
        private const val TAG = "DefaultAttachmentsHandler"
    }
}
