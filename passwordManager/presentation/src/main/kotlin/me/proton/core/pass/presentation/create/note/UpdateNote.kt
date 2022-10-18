package me.proton.core.pass.presentation.create.note

import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.pass.domain.ItemId
import me.proton.core.pass.domain.ShareId
import me.proton.core.pass.presentation.R
import me.proton.core.pass.presentation.components.common.PassSnackbarHost
import me.proton.core.pass.presentation.components.common.PassSnackbarHostState

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun UpdateNote(
    modifier: Modifier = Modifier,
    onUpClick: () -> Unit,
    onSuccess: (ShareId, ItemId) -> Unit,
    viewModel: UpdateNoteViewModel = hiltViewModel()
) {
    val noteUiState by viewModel.noteUiState.collectAsStateWithLifecycle()
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { PassSnackbarHostState() }
    val snackbarMessages = NoteSnackbarMessage.values()
        .associateWith { stringResource(id = it.id) }
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage
            .collectLatest { message ->
                coroutineScope.launch {
                    snackbarMessages[message]?.let {
                        snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, it)
                    }
                }
            }
    }
    Scaffold(
        snackbarHost = { PassSnackbarHost(snackbarHostState = snackbarHostState) }
    ) { innerPadding ->
        NoteContent(
            modifier = modifier.padding(innerPadding),
            uiState = noteUiState,
            topBarTitle = R.string.title_edit_note,
            topBarActionName = R.string.action_save,
            onUpClick = onUpClick,
            onSuccess = onSuccess,
            onSubmit = { shareId -> viewModel.updateItem(shareId) },
            onTitleChange = { viewModel.onTitleChange(it) },
            onNoteChange = { viewModel.onNoteChange(it) },
            onSnackbarMessage = { message ->
                coroutineScope.launch {
                    snackbarMessages[message]?.let {
                        snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, it)
                    }
                }
            }
        )
    }
}
