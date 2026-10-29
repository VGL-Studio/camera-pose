package com.vglstudio.camera.pose.line.guide.feature.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.camera.pose.line.guide.core.ads.PrivacyOptionsButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing

/** Stateful entry point used by navigation. Connects the ViewModel to the stateless screen. */
@Composable
internal fun SampleRoute(viewModel: SampleViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SampleScreen(uiState = uiState, onIncrement = viewModel::onIncrement)
}

/** Stateless screen: everything it shows comes from [uiState]. Tested on the JVM and by screenshots. */
@Composable
internal fun SampleScreen(uiState: SampleUiState, onIncrement: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize().testTag(SampleTestTags.SCREEN)) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.sample_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            when (uiState) {
                SampleUiState.Loading -> CircularProgressIndicator()
                SampleUiState.Disabled -> Text(text = stringResource(R.string.sample_disabled))
                is SampleUiState.Ready -> {
                    Text(text = stringResource(R.string.sample_count_label))
                    Text(
                        text = uiState.count.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.testTag(SampleTestTags.COUNT),
                    )
                    PrimaryButton(
                        text = stringResource(R.string.sample_increment),
                        onClick = onIncrement,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (uiState.saveFailed) {
                        Text(
                            text = stringResource(R.string.sample_save_failed),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(SampleTestTags.SAVE_ERROR),
                        )
                    }
                }
            }
            // Only visible where the consent form applies; an app usually puts it in its settings screen.
            PrivacyOptionsButton()
        }
    }
}

internal object SampleTestTags {
    const val SCREEN = "sample_screen"
    const val COUNT = "sample_count"
    const val SAVE_ERROR = "sample_save_error"
}

@Preview
@Composable
private fun SampleScreenPreview() {
    AppTheme {
        SampleScreen(uiState = SampleUiState.Ready(count = 3), onIncrement = {})
    }
}
