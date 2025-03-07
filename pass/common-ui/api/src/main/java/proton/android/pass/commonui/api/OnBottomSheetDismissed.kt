/*
 * Copyright (c) 2024-2025 Proton AG
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

package proton.android.pass.commonui.api

import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import proton.android.pass.log.api.PassLogger

fun onBottomSheetDismissed(
    coroutineScope: CoroutineScope,
    modalBottomSheetState: ModalBottomSheetState,
    dismissJob: MutableState<Job?>,
    block: () -> Unit
) {
    if (dismissJob.value?.isActive == true) return
    dismissJob.value = coroutineScope.launch {
        try {
            while (isActive && modalBottomSheetState.isVisible) {
                try {
                    modalBottomSheetState.hide()
                } catch (e: CancellationException) {
                    PassLogger.d(TAG, e, "Bottom sheet hidden animation interrupted")
                }
                delay(DELAY)
            }
            block()
        } finally {
            dismissJob.value = null
        }
    }
}

private const val DELAY = 10L
private const val TAG = "OnBottomSheetDismissed"
