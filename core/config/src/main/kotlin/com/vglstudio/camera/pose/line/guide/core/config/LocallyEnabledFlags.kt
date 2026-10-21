package com.vglstudio.camera.pose.line.guide.core.config

import javax.inject.Qualifier

/**
 * Qualifier for the set of flags turned on locally for one build, without any remote value.
 * The app's `staging` source set contributes to this set, so features under development are visible
 * on staging builds and stay off in production builds.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocallyEnabledFlags
