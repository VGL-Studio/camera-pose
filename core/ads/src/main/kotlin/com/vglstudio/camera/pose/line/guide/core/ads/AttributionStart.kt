package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.analytics.AttributionConsent
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

/**
 * Waits until AppsFlyer may start (ADR-0027, decision 3; plan T-033, section 2), the first of:
 *
 * - (a) the ads SDK was started ([sdkStarted]): the consent gate opened for the first time;
 * - (b) the consent round has an answer ([consentAnswered]): finished and settled, a refusal too;
 * - (c) [waitMillis] passed and no consent form is open ([consentFormOpen]); a form open then is waited for, and its
 *   close starts AppsFlyer with the consent state there is.
 *
 * A round that ended because the update failed or the form was interrupted is no answer: it goes the way of (c). One
 * function for the three paths (L-072).
 */
internal suspend fun awaitAttributionStart(
    sdkStarted: Flow<Boolean>,
    consentAnswered: Flow<Boolean>,
    consentFormOpen: Flow<Boolean>,
    waitMillis: Long,
) {
    merge(
        sdkStarted.filter { it },
        consentAnswered.filter { it },
        flow {
            delay(waitMillis)
            emit(consentFormOpen.first { !it })
        },
    ).first()
}

/**
 * The consent of the user is known for the attribution SDKs (plan T-099, step 7a; spec ads, question 21 i): the
 * round [finished] and either settled (a refusal and "no need to ask" too) or ended with the consent of an earlier
 * session kept ([ConsentPath.CACHED_EARLY] after a failed update). CACHED_EARLY while the round runs is not known: the
 * gate sets it when the request is sent, before UMP answers, and it turns to DENIED_OR_REQUIRED when that consent
 * expired. The 10 s path, ERROR_ALLOWED and INTERRUPTED_REGATHER are no consent known.
 */
internal fun attributionConsentKnown(finished: Boolean, state: ConsentGateState): Boolean =
    finished && (state.settled || state.path == ConsentPath.CACHED_EARLY)

/** Waits until [attributionConsentKnown] holds for the latest values of [finished] and [state]. */
internal suspend fun awaitAttributionConsentKnown(finished: Flow<Boolean>, state: Flow<ConsentGateState>) {
    combine(finished, state, ::attributionConsentKnown).first { it }
}

/** Waits until the consent is known ([awaitAttributionConsentKnown]), then tells [consent], once. */
internal suspend fun notifyAttributionConsentKnown(
    finished: Flow<Boolean>,
    state: Flow<ConsentGateState>,
    consent: AttributionConsent,
    logger: AppLogger,
    tag: String,
) {
    awaitAttributionConsentKnown(finished, state)
    logger.info(tag, "Attribution consent known")
    consent.onConsentKnown()
}
