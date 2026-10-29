package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity

/**
 * The User Messaging Platform in memory: records every call and its activity; callbacks run only when the test calls
 * [completeUpdate], [failUpdate] or [closeForm]. [canRequest], [formRequired], [privacyRequired] are set by the test.
 */
internal class FakeConsentPlatform : ConsentPlatform {
    var canRequest = false
    var formRequired = false
    var privacyRequired = false

    /** Every call by name, in order: requestUpdate, showFormIfRequired, showPrivacyForm, reset, canRequestAds. */
    val calls = mutableListOf<String>()

    /** The activity of every requestUpdate. */
    val updateActivities = mutableListOf<Activity>()

    /** How many times the request parameters were built (built once, on the first update, S-17). */
    var parametersBuilt = 0
        private set

    /** The message UMP would give with an error; it must never be logged (S-13). */
    val errorMessage = "secret-message"

    /** Runs inside showFormIfRequired and showPrivacyForm, so a test can read state while the form opens. */
    var onFormShown: () -> Unit = {}

    private var pendingSuccess: (() -> Unit)? = null
    private var pendingFailure: ((Int) -> Unit)? = null
    private var pendingForm: ((Int?) -> Unit)? = null

    fun count(name: String): Int = calls.count { it == name }

    override fun canRequestAds(): Boolean {
        calls += "canRequestAds"
        return canRequest
    }

    override fun consentFormRequired(): Boolean = formRequired

    override fun privacyOptionsRequired(): Boolean = privacyRequired

    override fun requestUpdate(activity: Activity, onSuccess: () -> Unit, onFailure: (errorCode: Int) -> Unit) {
        if (parametersBuilt == 0) parametersBuilt++
        calls += "requestUpdate"
        updateActivities += activity
        pendingSuccess = onSuccess
        pendingFailure = onFailure
    }

    override fun showFormIfRequired(activity: Activity, onClosed: (errorCode: Int?) -> Unit) {
        calls += "showFormIfRequired"
        pendingForm = onClosed
        onFormShown()
    }

    override fun showPrivacyForm(activity: Activity, onClosed: (errorCode: Int?) -> Unit) {
        calls += "showPrivacyForm"
        pendingForm = onClosed
        onFormShown()
    }

    override fun reset() {
        calls += "reset"
    }

    fun completeUpdate() {
        val callback = checkNotNull(pendingSuccess) { "no update in progress" }
        pendingSuccess = null
        pendingFailure = null
        callback()
    }

    fun failUpdate(code: Int) {
        val callback = checkNotNull(pendingFailure) { "no update in progress" }
        pendingSuccess = null
        pendingFailure = null
        callback(code)
    }

    /** Closes the open form; [twice] calls the callback a second time, as a faulty SDK could. */
    fun closeForm(code: Int?, twice: Boolean = false) {
        val callback = checkNotNull(pendingForm) { "no form open" }
        pendingForm = null
        callback(code)
        if (twice) callback(code)
    }
}
