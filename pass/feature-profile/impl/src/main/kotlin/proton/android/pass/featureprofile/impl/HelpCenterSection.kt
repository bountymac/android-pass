package proton.android.pass.featureprofile.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallWeak
import proton.android.pass.commonui.api.PassTheme
import proton.android.pass.commonui.api.ThemePreviewProvider
import proton.android.pass.composecomponents.impl.container.roundedContainer

@Composable
fun HelpCenterSection(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.profile_help_center),
            style = ProtonTheme.typography.defaultSmallWeak
        )
        Column(
            modifier = Modifier.roundedContainer(ProtonTheme.colors.separatorNorm)
        ) {
            ProfileOption(text = stringResource(R.string.profile_option_tips), onClick = {})
            Divider()
            ProfileOption(text = stringResource(R.string.profile_option_feedback), onClick = {})
            Divider()
            ProfileOption(text = stringResource(R.string.profile_option_rating), onClick = {})
        }
    }
}

@Preview
@Composable
fun HelpCenterSectionPreview(
    @PreviewParameter(ThemePreviewProvider::class) isDark: Boolean
) {
    PassTheme(isDark = isDark) {
        Surface {
            HelpCenterSection()
        }
    }
}
