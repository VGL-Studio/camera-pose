package com.vglstudio.camera.pose.line.guide.core.config

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Web pages the app opens, set remotely without a new release (spec settings, KB-5). */
interface AppLinks {
    /** The privacy policy page; always an https address. */
    val privacyPolicyUrl: Flow<String>
}

/** Skeleton for the tests written first (plan T-025, section 4). */
internal class DefaultAppLinks @Inject constructor(@Suppress("unused") private val remoteSettings: RemoteSettings) :
    AppLinks {
    override val privacyPolicyUrl: Flow<String> = flowOf("")
}
