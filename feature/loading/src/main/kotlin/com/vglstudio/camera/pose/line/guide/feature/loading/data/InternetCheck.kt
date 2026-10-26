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

    // Skeleton of the tests-first step: not implemented yet.
    override fun isInternetValidated(): Boolean = false
}

/** Skeleton of the tests-first step: not implemented yet. */
internal fun NetworkCapabilities?.isValidatedInternet(): Boolean = false
