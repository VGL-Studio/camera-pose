package com.vglstudio.camera.pose.line.guide.feature.camera.review

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareIntentsTest {
    @Test
    fun chooser_wrapsSendWithStreamAndGrant() {
        val uri = Uri.parse("content://com.example.captures/captures/capture_1.jpg")

        val chooser = shareChooser(uri, "Chia sẻ ảnh")

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals("Chia sẻ ảnh", chooser.getCharSequenceExtra(Intent.EXTRA_TITLE))
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(send)
        send!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("image/jpeg", send.type)
        @Suppress("DEPRECATION")
        assertEquals(uri, send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val clip = send.clipData
        assertNotNull(clip)
        assertEquals(uri, clip!!.getItemAt(0).uri)
    }
}
