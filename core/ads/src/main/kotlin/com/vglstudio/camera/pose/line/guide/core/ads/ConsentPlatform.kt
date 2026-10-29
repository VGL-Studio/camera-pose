package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/** The User Messaging Platform, behind an interface so the consent rules run on the JVM ([ConsentCoordinator]). */
internal interface ConsentPlatform {
    fun canRequestAds(): Boolean

    /** consentStatus == REQUIRED: the consent form will really show (S-16). */
    fun consentFormRequired(): Boolean

    /** privacyOptionsRequirementStatus == REQUIRED (S-14). */
    fun privacyOptionsRequired(): Boolean

    fun requestUpdate(activity: Activity, onSuccess: () -> Unit, onFailure: (errorCode: Int) -> Unit)

    fun showFormIfRequired(activity: Activity, onClosed: (errorCode: Int?) -> Unit)

    fun showPrivacyForm(activity: Activity, onClosed: (errorCode: Int?) -> Unit)

    /** S-18. */
    fun reset()
}

/**
 * [ConsentPlatform] over the User Messaging Platform, the only place :core:ads calls UMP (ADR-0013). The request
 * parameters are built once, from [debug], and reused by every regather (S-17). No unit test: it calls UMP directly,
 * like [SdkNativeAdLoader]; checked by hand on staging (plan T-020, section 4).
 */
internal class UmpConsentPlatform(private val context: Context, private val debug: ConsentDebugOptions) :
    ConsentPlatform {
    private val information: ConsentInformation by lazy { UserMessagingPlatform.getConsentInformation(context) }
    private var parameters: ConsentRequestParameters? = null

    override fun canRequestAds(): Boolean = information.canRequestAds()

    override fun consentFormRequired(): Boolean = information.consentStatus == ConsentInformation.ConsentStatus.REQUIRED

    override fun privacyOptionsRequired(): Boolean =
        information.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    override fun requestUpdate(activity: Activity, onSuccess: () -> Unit, onFailure: (errorCode: Int) -> Unit) {
        information.requestConsentInfoUpdate(
            activity,
            parameters(activity),
            { onSuccess() },
            { error -> onFailure(error.errorCode) },
        )
    }

    override fun showFormIfRequired(activity: Activity, onClosed: (errorCode: Int?) -> Unit) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error -> onClosed(error?.errorCode) }
    }

    override fun showPrivacyForm(activity: Activity, onClosed: (errorCode: Int?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error -> onClosed(error?.errorCode) }
    }

    override fun reset() {
        information.reset()
    }

    private fun parameters(activity: Activity): ConsentRequestParameters = parameters ?: run {
        val builder = ConsentRequestParameters.Builder()
        if (debug.forceEea) {
            builder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .setForceTesting(true)
                    .build(),
            )
        }
        builder.build().also { parameters = it }
    }
}
