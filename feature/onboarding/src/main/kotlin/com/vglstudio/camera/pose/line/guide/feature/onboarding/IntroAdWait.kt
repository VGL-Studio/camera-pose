package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.compose.runtime.Composable

/** The longest page one holds Next while the step card waits for an ad (spec onboarding, KB-6). */
internal const val INTRO_AD_WAIT_MILLIS = 5_000L

/** Why the hold of page one ended, as logged (plan T-083). */
internal enum class IntroAdWaitEnd(val logName: String) {
    AD("ad"),
    TIMEOUT("timeout"),
    NOT_ALLOWED("not allowed"),
    LEFT_PAGE("left page"),
    BACKGROUND("background"),
    DISPOSED("disposed"),
}

/** Skeleton of plan T-083, step 3: never holds. */
@Composable
internal fun rememberIntroAdWait(
    onIntro: Boolean,
    allowed: Boolean,
    hasAd: Boolean,
    onStarted: () -> Unit = {},
    onEnded: (IntroAdWaitEnd) -> Unit = {},
    maxMillis: Long = INTRO_AD_WAIT_MILLIS,
): Boolean = false
