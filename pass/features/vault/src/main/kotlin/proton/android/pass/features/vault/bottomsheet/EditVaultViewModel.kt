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

package proton.android.pass.features.vault.bottomsheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import proton.android.pass.common.api.LoadingResult
import proton.android.pass.common.api.asLoadingResult
import proton.android.pass.composecomponents.impl.uievents.IsLoadingState
import proton.android.pass.crypto.api.context.EncryptionContextProvider
import proton.android.pass.data.api.usecases.GetVaultByShareId
import proton.android.pass.data.api.usecases.UpdateVault
import proton.android.pass.domain.ShareId
import proton.android.pass.domain.Vault
import proton.android.pass.domain.entity.NewVault
import proton.android.pass.features.vault.VaultSnackbarMessage
import proton.android.pass.log.api.PassLogger
import proton.android.pass.navigation.api.CommonNavArgId
import proton.android.pass.notifications.api.SnackbarDispatcher
import javax.inject.Inject

@HiltViewModel
class EditVaultViewModel @Inject constructor(
    private val snackbarDispatcher: SnackbarDispatcher,
    private val updateVault: UpdateVault,
    private val encryptionContextProvider: EncryptionContextProvider,
    private val getVaultByShareId: GetVaultByShareId,
    savedStateHandle: SavedStateHandle
) : BaseVaultViewModel() {

    private val shareId =
        ShareId(requireNotNull(savedStateHandle.get<String>(CommonNavArgId.ShareId.key)))

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        PassLogger.w(TAG, throwable)
    }

    fun onStart() = viewModelScope.launch(coroutineExceptionHandler) {
        formFlow.update { CreateVaultFormValues() }

        getVaultByShareId(shareId = shareId)
            .asLoadingResult()
            .collect {
                when (it) {
                    LoadingResult.Loading -> {
                        isLoadingFlow.update { IsLoadingState.Loading }
                    }

                    is LoadingResult.Error -> {
                        PassLogger.w(TAG, "Error getting vault by id")
                        PassLogger.w(TAG, it.exception)
                        snackbarDispatcher(VaultSnackbarMessage.CannotRetrieveVaultError)
                        isLoadingFlow.update { IsLoadingState.NotLoading }
                    }

                    is LoadingResult.Success -> {
                        setInitialValues(it.data)
                        isLoadingFlow.update { IsLoadingState.NotLoading }
                    }
                }
            }
    }

    fun onEditClick() = viewModelScope.launch {
        if (formFlow.value.name.isBlank()) return@launch

        isLoadingFlow.update { IsLoadingState.Loading }

        val form = formFlow.value
        val body = encryptionContextProvider.withEncryptionContext {
            NewVault(
                name = encrypt(form.name.trimEnd()),
                description = encrypt(""),
                icon = form.icon,
                color = form.color
            )
        }

        PassLogger.d(TAG, "Sending Edit Vault request")

        runCatching {
            updateVault(vault = body, shareId = shareId)
        }.onSuccess {
            PassLogger.d(TAG, "Vault edited successfully")
            snackbarDispatcher(VaultSnackbarMessage.EditVaultSuccess)
            isLoadingFlow.update { IsLoadingState.NotLoading }
            eventFlow.update { IsVaultCreatedEvent.Created }
        }.onFailure {
            PassLogger.w(TAG, "Edit Vault Failed")
            PassLogger.w(TAG, it, "Edit Vault Failed")
            snackbarDispatcher(VaultSnackbarMessage.EditVaultError)
            isLoadingFlow.update { IsLoadingState.NotLoading }
        }
    }

    private fun setInitialValues(vault: Vault) {
        formFlow.update {
            CreateVaultFormValues(
                name = vault.name,
                icon = vault.icon,
                color = vault.color
            )
        }
    }

    companion object {
        private const val TAG = "EditVaultViewModel"
    }
}
