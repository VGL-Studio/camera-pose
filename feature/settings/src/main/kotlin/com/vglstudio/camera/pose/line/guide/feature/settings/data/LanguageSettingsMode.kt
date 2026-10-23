package com.vglstudio.camera.pose.line.guide.feature.settings.data

import android.os.Build

/**
 * Where the app language is chosen: from Android 13 on the app language page of the device settings, below that in
 * the picker of the app (spec settings, KB-4).
 */
fun interface LanguageSettingsMode {
    fun usesSystemPage(): Boolean
}

internal object SdkLanguageSettingsMode : LanguageSettingsMode {
    override fun usesSystemPage(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
