package com.vglstudio.camera.pose.line.guide.feature.settings.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The version name of the installed app, shown on the "Version" row (spec settings, KB-6). */
fun interface AppVersionProvider {
    fun versionName(): String
}

internal class PackageManagerAppVersionProvider @Inject constructor(@ApplicationContext private val context: Context) :
    AppVersionProvider {
    override fun versionName(): String {
        val packageManager = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, 0)
        }
        return info.versionName.orEmpty()
    }
}
