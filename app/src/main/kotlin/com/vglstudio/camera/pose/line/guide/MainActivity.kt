package com.vglstudio.camera.pose.line.guide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.vglstudio.camera.pose.line.guide.core.ads.AdsController
import com.vglstudio.camera.pose.line.guide.core.ads.LocalAdsController
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.network.NetworkDialogController
import com.vglstudio.camera.pose.line.guide.navigation.AppNavHost
import com.vglstudio.camera.pose.line.guide.navigation.MainScreenChooser
import com.vglstudio.camera.pose.line.guide.network.NetworkDialogHost
import com.vglstudio.camera.pose.line.guide.onboarding.OnboardingStatus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var ads: AdsController

    @Inject
    lateinit var networkDialog: NetworkDialogController

    @Inject
    internal lateinit var mainScreenChooser: MainScreenChooser

    @Inject
    internal lateinit var onboardingStatus: OnboardingStatus

    @Inject
    lateinit var featureFlags: FeatureFlags

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Shows the consent form when required; the ads SDK starts only after it (ADR-0004).
        ads.gatherConsent(this)
        setContent {
            CompositionLocalProvider(LocalAdsController provides ads) {
                AppTheme {
                    // The surface fills the whole window, including the area behind the system bars,
                    // so those areas take the theme background in both light and dark mode.
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val networkDialogState by networkDialog.state.collectAsState()
                        // Followed, not read once: the library button shows and hides with the remote value (ADR-0003).
                        val posesEnabled by featureFlags.enabled(FeatureFlag.POSES).collectAsState(initial = false)
                        // Back while the network dialog shows sends the app to the background (spec network, KB-3).
                        NetworkDialogHost(
                            state = networkDialogState,
                            onRetry = networkDialog::retry,
                            onBack = { moveTaskToBack(true) },
                        ) {
                            AppNavHost(
                                chooseMainScreen = mainScreenChooser::choose,
                                markOnboardingDone = onboardingStatus::markDone,
                                onPastLoadingChanged = networkDialog::setActive,
                                posesEnabled = posesEnabled,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        ads.onUserInteraction()
    }
}
