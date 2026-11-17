package com.vglstudio.camera.pose.line.guide.core.analytics

/**
 * The consent of the user is known for the attribution SDKs (spec ads, question 21 i; ADR-1010): the consent round
 * finished with an answer that holds (a refusal and "no need to ask" too), or consent of an earlier session was kept.
 * :core:ads calls [onConsentKnown] once per process; before it, Tenjin neither connects nor sends.
 */
interface AttributionConsent {
    fun onConsentKnown()
}
