package me.proton.pass.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import me.proton.android.pass.preferences.ThemePreference
import me.proton.android.pass.ui.shared.HamburgerIcon
import me.proton.android.pass.ui.shared.TopBarTitleView
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.pass.presentation.R
import me.proton.pass.presentation.components.common.LoadingDialog
import me.proton.pass.presentation.components.common.bottomsheet.PassModalBottomSheetLayout
import me.proton.pass.presentation.uievents.IsButtonEnabled
import me.proton.pass.presentation.uievents.IsLoadingState

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    state: SettingsUiState,
    appVersion: String,
    onThemeChange: (ThemePreference) -> Unit,
    onFingerPrintLockChange: (IsButtonEnabled) -> Unit,
    onDrawerIconClick: () -> Unit,
    onToggleAutofillChange: (Boolean) -> Unit,
    onForceSyncClick: () -> Unit,
    onAppVersionClick: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

    BackHandler(
        enabled = bottomSheetState.isVisible
    ) {
        scope.launch { bottomSheetState.hide() }
    }

    PassModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            ThemeSelectionBottomSheetContents(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
                onThemeSelected = { theme ->
                    scope.launch {
                        bottomSheetState.hide()
                        onThemeChange(theme)
                    }
                }
            )
        }
    ) {
        Scaffold(
            modifier = modifier,
            scaffoldState = scaffoldState,
            topBar = {
                ProtonTopAppBar(
                    title = {
                        TopBarTitleView(
                            title = stringResource(id = R.string.title_settings)
                        )
                    },
                    navigationIcon = {
                        HamburgerIcon(
                            onClick = onDrawerIconClick
                        )
                    }
                )
            }
        ) { contentPadding ->
            if (state.isLoadingState == IsLoadingState.Loading) {
                LoadingDialog()
            }
            Settings(
                modifier = Modifier.padding(contentPadding),
                state = state,
                appVersion = appVersion,
                onOpenThemeSelection = { scope.launch { bottomSheetState.show() } },
                onFingerPrintLockChange = onFingerPrintLockChange,
                onToggleAutofillChange = onToggleAutofillChange,
                onForceSyncClick = onForceSyncClick,
                onAppVersionClick = onAppVersionClick
            )
        }
    }
}
