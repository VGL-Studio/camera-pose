package com.vglstudio.camera.pose.line.guide.core.assets

import com.vglstudio.camera.pose.line.guide.core.logging.LogEntry
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DownloadFailureLogTest {
    private val path = AssetPath("poses/catalog/v1/catalog-ab12cd34.json")

    @Test
    fun unknownHost_namesTheErrorAndNetworkReason() {
        assertEquals(
            "Could not download poses/catalog/v1/catalog-ab12cd34.json (reason=NETWORK, error=UnknownHostException)",
            downloadFailureMessage(path, UnknownHostException("cdn.example")),
        )
    }

    @Test
    fun wrappedCause_listsTheChain() {
        assertEquals(
            "Could not download poses/catalog/v1/catalog-ab12cd34.json " +
                "(reason=NETWORK, error=IOException <- SocketTimeoutException)",
            downloadFailureMessage(path, IOException("x", SocketTimeoutException())),
        )
    }

    @Test
    fun httpStatus_isWritten() {
        val message = downloadFailureMessage(path, HttpStatusException(404, "https://cdn.example/a/v1/x.json"))

        assertEquals(
            "Could not download poses/catalog/v1/catalog-ab12cd34.json " +
                "(reason=NOT_FOUND, error=HttpStatusException, status=404)",
            message,
        )
        // The error message holds the full address: it must not be logged.
        assertFalse(message.contains("https://"))
    }

    @Test
    fun longChain_stopsAtFiveClasses() {
        val error = (1..6).fold(IOException("root")) { cause, i -> IOException("level $i", cause) }

        val message = downloadFailureMessage(path, error)

        assertEquals(4, message.split(" <- ").size - 1)
    }

    @Test
    fun logger_writesOneWarningUnderAssets() {
        val logger = RecordingAppLogger()
        val error = UnknownHostException("cdn.example")

        downloadFailureLogger(logger)(path, error)

        assertEquals(
            listOf(LogEntry(LogLevel.WARN, "Assets", downloadFailureMessage(path, error), error)),
            logger.entries,
        )
    }
}
