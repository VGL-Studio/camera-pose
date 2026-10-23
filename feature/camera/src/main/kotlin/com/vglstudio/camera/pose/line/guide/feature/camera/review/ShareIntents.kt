package com.vglstudio.camera.pose.line.guide.feature.camera.review

import android.content.ClipData
import android.content.Intent
import android.net.Uri

/**
 * The system share sheet for one JPEG photo. The clip data and the read grant let the receiving app open [uri],
 * a FileProvider uri of the cache file or the MediaStore uri of the saved photo.
 */
internal fun shareChooser(uri: Uri, title: String): Intent {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = MIME_JPEG
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, title)
}

private const val MIME_JPEG = "image/jpeg"
