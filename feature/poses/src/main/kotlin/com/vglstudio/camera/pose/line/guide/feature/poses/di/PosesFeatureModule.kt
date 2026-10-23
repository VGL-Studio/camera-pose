package com.vglstudio.camera.pose.line.guide.feature.poses.di

import javax.inject.Qualifier

/** The dispatcher that decodes thumbnails: Dispatchers.IO in the app, a test dispatcher in tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class PosesIoDispatcher
