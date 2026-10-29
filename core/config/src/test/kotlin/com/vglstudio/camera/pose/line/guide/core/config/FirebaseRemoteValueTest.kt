package com.vglstudio.camera.pose.line.guide.core.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_REMOTE
import com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_STATIC
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseRemoteValueTest {
    private val wrongTypeErrors = mutableListOf<IllegalArgumentException>()
    private val recordWrongType: (IllegalArgumentException) -> Unit = { wrongTypeErrors += it }
    private var convertCalls = 0

    private fun convertTo(value: Double): () -> Double = {
        convertCalls++
        value
    }

    @Test
    fun staticSource_returnsNull() {
        assertNull(activeValueOrNull(VALUE_SOURCE_STATIC, convertTo(0.25), recordWrongType))
        assertEquals(0, convertCalls)
        // Control: the same value from the server is read, so null above comes from the source, not the value.
        assertEquals(0.25, activeValueOrNull(VALUE_SOURCE_REMOTE, convertTo(0.25), recordWrongType)!!, 0.0)
    }

    @Test
    fun remoteSource_returnsConvertedValue() {
        val value = activeValueOrNull(VALUE_SOURCE_REMOTE, convertTo(0.25), recordWrongType)
        assertEquals(0.25, value!!, 0.0)
        assertEquals(1, convertCalls)
        assertTrue(wrongTypeErrors.isEmpty())
    }

    @Test
    fun wrongType_returnsNullAndReportsIt() {
        val value = activeValueOrNull<Double>(
            VALUE_SOURCE_REMOTE,
            { throw IllegalArgumentException("not a double") },
            recordWrongType,
        )
        assertNull(value)
        assertEquals(1, wrongTypeErrors.size)
        assertTrue(wrongTypeErrors.single() is IllegalArgumentException)
    }
}
