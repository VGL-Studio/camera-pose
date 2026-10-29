package com.vglstudio.camera.pose.line.guide

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.vglstudio.camera.pose.line.guide.core.ads.AdsController
import com.vglstudio.camera.pose.line.guide.core.ads.AdsHost
import com.vglstudio.camera.pose.line.guide.core.ads.InterstitialBreakGate
import com.vglstudio.camera.pose.line.guide.core.ads.LocalInterstitialBreakGate
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode
import com.vglstudio.camera.pose.line.guide.core.data.UserPreferences
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.network.NetworkDialogController
import com.vglstudio.camera.pose.line.guide.core.review.InAppReview
import com.vglstudio.camera.pose.line.guide.core.review.LocalInAppReview
import com.vglstudio.camera.pose.line.guide.navigation.AppNavHost
import com.vglstudio.camera.pose.line.guide.navigation.MainScreenChooser
import com.vglstudio.camera.pose.line.guide.network.NetworkDialogHost
import com.vglstudio.camera.pose.line.guide.onboarding.OnboardingStatus
import com.vglstudio.camera.pose.line.guide.settings.isDark
import com.vglstudio.camera.pose.line.guide.settings.languageToApply
import com.vglstudio.camera.pose.line.guide.settings.readStoredLanguage
import com.vglstudio.camera.pose.line.guide.settings.withAppLanguage
import com.vglstudio.camera.pose.line.guide.ui.APP_ROOT_TAG
import com.vglstudio.camera.pose.line.guide.ui.applyScreenOrientation
import com.vglstudio.camera.pose.line.guide.ui.exposeTestTagsForUiAutomator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

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

    @Inject
    lateinit var inAppReview: InAppReview

    @Inject
    lateinit var interstitialBreakGate: InterstitialBreakGate

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var logger: AppLogger

    // Below Android 13 the language chosen in the app is applied here, before anything reads a resource (spec
    // settings, KB-4). From Android 13 the system applies it and nothing is read.
    override fun attachBaseContext(newBase: Context) {
        val storedLanguage = readStoredLanguage(newBase)
        super.attachBaseContext(newBase.withAppLanguage(languageToApply(Build.VERSION.SDK_INT, storedLanguage)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Portrait on a phone, the device's orientation on a large screen (FB-20261024-01, plan T-056).
        applyScreenOrientation(logger)
        enableEdgeToEdge()
        // Shows the consent form when required; the ads SDK starts only after it (ADR-0004).
        ads.gatherConsent(this)
        // Read once, so the first frame already has the chosen look instead of flashing (plan T-025, question 6).
        val initialThemeMode = runBlocking {
            withTimeoutOrNull(THEME_READ_TIMEOUT_MILLIS) { userPreferences.themeMode.first() }
        } ?: ThemeMode.SYSTEM
        setContent {
            // The look chosen on the settings screen, applied at once to every screen (spec settings, KB-2).
            val themeMode by userPreferences.themeMode.collectAsState(initial = initialThemeMode)
            val darkTheme = themeMode.isDark(systemDark = isSystemInDarkTheme())
            // The status bar icons follow the look of the app, not the device setting.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }
            AppAdsRoot(
                ads = ads,
                inAppReview = inAppReview,
                breakGate = interstitialBreakGate,
                fullScreenAdShowing = ads.fullScreenAdShowing,
                onFullScreenAdShowingChange = networkDialog::setFullScreenAdShowing,
            ) {
                AppTheme(darkTheme = darkTheme) {
                    // The surface fills the whole window, including the area behind the system bars,
                    // so those areas take the theme background in both light and dark mode.
                    Surface(modifier = Modifier.fillMaxSize().exposeTestTagsForUiAutomator().testTag(APP_ROOT_TAG)) {
                        val networkDialogState by networkDialog.state.collectAsState()
                        // Followed, not read once: the library button shows and hides with the remote value (ADR-0003).
                        val posesEnabled by featureFlags.enabled(FeatureFlag.POSES).collectAsState(initial = false)
                        val settingsEnabled by featureFlags.enabled(FeatureFlag.SETTINGS)
                            .collectAsState(initial = false)
                        // Back while the network dialog shows sends the app to the background (spec network, KB-3).
                        NetworkDialogHost(
                            state = networkDialogState,
                            onRetry = networkDialog::retry,
                            onBack = { moveTaskToBack(true) },
                            darkTheme = darkTheme,
                        ) {
                            AppNavHost(
                                chooseMainScreen = mainScreenChooser::choose,
                                markOnboardingDone = onboardingStatus::markDone,
                                onPastLoadingChanged = networkDialog::setActive,
                                posesEnabled = posesEnabled,
                                settingsEnabled = settingsEnabled,
                            )
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val THEME_READ_TIMEOUT_MILLIS = 200L

        // The navigation bar scrims of enableEdgeToEdge() (androidx.activity defaults, not exposed by the library).
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}

/**
 * The root of the app's UI for ads (plan T-074): [AdsHost] provides the ads controller and reports the hold of the ads
 * that show by themselves (G-04); inside it every screen reads the break limits of review_interstitial and the review
 * entry. MainActivity sets it on every create, so a recreated activity has it too (L-061).
 */
@Composable
internal fun AppAdsRoot(
    ads: AdsController?,
    inAppReview: InAppReview,
    breakGate: InterstitialBreakGate,
    // Skeleton for the tests written first (plan T-074, pass 2): not followed yet.
    @Suppress("UNUSED_PARAMETER") fullScreenAdShowing: StateFlow<Boolean>,
    @Suppress("UNUSED_PARAMETER") onFullScreenAdShowingChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    AdsHost(ads) {
        CompositionLocalProvider(
            LocalInAppReview provides inAppReview,
            LocalInterstitialBreakGate provides breakGate,
        ) {
            content()
        }
    }
}
