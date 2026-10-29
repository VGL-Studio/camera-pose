package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
