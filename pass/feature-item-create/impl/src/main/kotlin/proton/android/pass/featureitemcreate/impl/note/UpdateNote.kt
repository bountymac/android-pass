package proton.android.pass.featureitemcreate.impl.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import proton.android.pass.composecomponents.impl.dialogs.ConfirmCloseDialog
import proton.android.pass.featureitemcreate.impl.R
import proton.pass.domain.ItemId
import proton.pass.domain.ShareId

@OptIn(ExperimentalLifecycleComposeApi::class, ExperimentalComposeUiApi::class)
@Composable
fun UpdateNote(
    modifier: Modifier = Modifier,
    onUpClick: () -> Unit,
    onSuccess: (ShareId, ItemId) -> Unit,
    viewModel: UpdateNoteViewModel = hiltViewModel()
) {
    val noteUiState by viewModel.updateNoteUiState.collectAsStateWithLifecycle()

    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }
    val onExit = {
        if (noteUiState.baseNoteUiState.hasUserEditedContent) {
            showConfirmDialog = !showConfirmDialog
        } else {
            onUpClick()
        }
    }
    BackHandler {
        onExit()
    }

    Box(modifier = modifier.fillMaxSize()) {
        NoteContent(
            uiState = noteUiState.baseNoteUiState,
            selectedShareId = noteUiState.selectedShareId,
            topBarActionName = stringResource(R.string.action_save),
            onUpClick = onExit,
            onSuccess = { shareId, itemId -> onSuccess(shareId, itemId) },
            onSubmit = { shareId -> viewModel.updateItem(shareId) },
            onTitleChange = { viewModel.onTitleChange(it) },
            onNoteChange = { viewModel.onNoteChange(it) },
            vaultSelect = {},
        )

        ConfirmCloseDialog(
            show = showConfirmDialog,
            onCancel = {
                showConfirmDialog = false
            },
            onConfirm = {
                showConfirmDialog = false
                onUpClick()
            }
        )
    }
}
