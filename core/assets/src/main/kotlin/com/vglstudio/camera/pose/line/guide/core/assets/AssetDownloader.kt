package com.vglstudio.camera.pose.line.guide.core.assets

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Downloads one file. Separate from [DefaultRemoteAssets] so the cache can be tested without a network. */
internal fun interface AssetDownloader {
    /** Writes the content at [url] into [target]. Throws [IOException] on any failure. */
    @Throws(IOException::class)
    fun download(url: String, target: File)
}

/** The server answered with a status other than 200. */
internal class HttpStatusException(val status: Int, url: String) : IOException("HTTP $status for $url")

/** Plain HttpURLConnection: enough for public files on R2, and no extra SDK in the app. */
internal class HttpAssetDownloader : AssetDownloader {
    override fun download(url: String, target: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) throw HttpStatusException(status, url)
            val expectedLength = connection.contentLengthLong
            val written = connection.inputStream.use { input -> target.outputStream().use { input.copyTo(it) } }
            if (expectedLength >= 0 && written != expectedLength) {
                throw IOException("Incomplete download of $url: $written of $expectedLength bytes")
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
    }
}
