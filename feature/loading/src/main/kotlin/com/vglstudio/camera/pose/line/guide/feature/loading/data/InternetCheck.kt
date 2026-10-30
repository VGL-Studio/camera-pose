package com.vglstudio.camera.pose.line.guide.feature.loading.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Tells whether the system has confirmed that the device reaches the Internet right now (plan T-057). */
fun interface InternetCheck {
    fun isInternetValidated(): Boolean
}

internal class AndroidInternetCheck(context: Context) : InternetCheck {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    override fun isInternetValidated(): Boolean =
        manager.getNetworkCapabilities(manager.activeNetwork).isValidatedInternet()
}

/**
 * True when the network can reach the Internet and the system has checked that it does. VALIDATED is needed: a
 * Wi-Fi network behind a sign-in page, or one without a way out, has INTERNET but not VALIDATED, and there "check
 * your network" is the right guidance. Same rule as AndroidConnectivitySource of :core:network (ADR-0024 point 1,
 * plan T-076).
 */
internal fun NetworkCapabilities?.isValidatedInternet(): Boolean = this != null &&
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
    hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
