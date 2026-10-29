package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import java.util.Collections
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * The AppsFlyer SDK in memory (ADR-0027, decision 5): records every call by name, in order, and every event sent.
 * [failure] makes every call throw it.
 */
internal class FakeAppsFlyerSink : AppsFlyerSink {
    /** `init`, `start`, `logEvent`, in order. */
    val calls = mutableListOf<String>()

    /** The events given to [logEvent], in order. */
    val events = mutableListOf<AfEvent>()

    /** The dev keys given to [init]. */
    val keys = mutableListOf<String>()

    var failure: Exception? = null

    override fun init(devKey: String) {
        calls += "init"
        keys += devKey
        failure?.let { throw it }
    }

    override fun start() {
        calls += "start"
        failure?.let { throw it }
    }

    override fun logEvent(name: String, params: Map<String, Any>?) {
        calls += "logEvent"
        failure?.let { throw it }
        events += AfEvent(name, params)
    }
}

/**
 * [RemoteSettings] for unit tests, copied from the one of :core:ads (the tests of this module do not see it): a value
 * set by key name, else the key's default; the flow emits the current value at once, then again on every change.
 */
internal class FakeRemoteSettings : RemoteSettings {
    private val values = MutableStateFlow<Map<String, Any>>(emptyMap())
    private val read = Collections.synchronizedSet(mutableSetOf<String>())

    /** Names of the keys read through [value] so far. */
    val readNames: Set<String> get() = synchronized(read) { read.toSet() }

    fun <T : Any> set(key: RemoteKey<T>, value: T) {
        values.update { it + (key.name to value) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> value(key: RemoteKey<T>): Flow<T> {
        read += key.name
        return values.map { (it[key.name] as T?) ?: key.default }
    }
}

/** A country that never changes. */
internal class FixedCountry(private val code: String) : CountrySource {
    override fun current(): String = code
}
