package com.vglstudio.camera.pose.line.guide.core.network.di

import android.content.Context
import com.vglstudio.camera.pose.line.guide.core.network.AndroidConnectivitySource
import com.vglstudio.camera.pose.line.guide.core.network.ConnectivitySource
import com.vglstudio.camera.pose.line.guide.core.network.DefaultInternetConnectivity
import com.vglstudio.camera.pose.line.guide.core.network.InternetConnectivity
import com.vglstudio.camera.pose.line.guide.core.network.NetworkDialogController
import com.vglstudio.camera.pose.line.guide.core.network.NetworkFailureReporter
import com.vglstudio.camera.pose.line.guide.core.network.NetworkStatus
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** The scope in which the network dialog controller runs, for the whole life of the app. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class NetworkScope

/** One controller serves every module: :app draws its state, other modules read or report through interfaces. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkBindingsModule {
    @Binds
    abstract fun networkStatus(controller: NetworkDialogController): NetworkStatus

    @Binds
    abstract fun networkFailureReporter(controller: NetworkDialogController): NetworkFailureReporter

    /** The Internet watch of other core modules, on the same source as the dialog (:core:ads, S-09). */
    @Binds
    abstract fun internetConnectivity(connectivity: DefaultInternetConnectivity): InternetConnectivity
}

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun connectivitySource(@ApplicationContext context: Context): ConnectivitySource =
        AndroidConnectivitySource(context)

    @Provides
    @Singleton
    @NetworkScope
    fun networkScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
