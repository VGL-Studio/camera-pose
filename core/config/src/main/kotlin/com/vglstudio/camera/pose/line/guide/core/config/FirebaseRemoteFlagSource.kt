package com.vglstudio.camera.pose.line.guide.core.config

import android.util.Log
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn

/**
 * Reads values from Firebase Remote Config (ADR-0003, ADR-0012).
 * A value is read at once from what is already activated (values fetched in an earlier session), so no
 * screen waits for the network. New values, from the start-up fetch or pushed in real time, are activated
 * and emitted when they arrive. A failed fetch keeps the current values.
 */
@Singleton
internal class FirebaseRemoteFlagSource @Inject constructor() : RemoteFlagSource {
    private val remoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    /** Emits each time new values are activated. Shared by every key: one fetch and one listener for all. */
    private val activations: SharedFlow<Unit> = callbackFlow {
        remoteConfig.fetchAndActivate().addOnCompleteListener { trySend(Unit) }
        val registration = remoteConfig.addOnConfigUpdateListener(
            object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    remoteConfig.activate().addOnCompleteListener { trySend(Unit) }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w(TAG, "Real-time update failed, keeping current values", error)
                }
            },
        )
        awaitClose { registration.remove() }
    }.shareIn(CoroutineScope(SupervisorJob() + Dispatchers.Default), SharingStarted.WhileSubscribed())

    override fun boolean(key: String): Flow<Boolean?> = values(key) { it.asBoolean() }

    override fun long(key: String): Flow<Long?> = values(key) { it.asLong() }

    override fun string(key: String): Flow<String?> = flowOf(null)

    private fun <T> values(key: String, convert: (FirebaseRemoteConfigValue) -> T): Flow<T?> = activations
        .onStart { emit(Unit) }
        .map { read(key, convert) }
        .distinctUntilChanged()

    private fun <T> read(key: String, convert: (FirebaseRemoteConfigValue) -> T): T? {
        val value = remoteConfig.getValue(key)
        if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) return null
        return try {
            convert(value)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Remote value of $key has the wrong type, using the default", e)
            null
        }
    }

    private companion object {
        const val TAG = "RemoteFlags"
    }
}
