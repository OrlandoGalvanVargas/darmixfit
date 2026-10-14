package com.darmix.darmixfit

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import com.darmix.darmixfit.data.AppDatabase
import com.darmix.darmixfit.data.RoutineRepository
import com.darmix.darmixfit.service.TimerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FitFlowApp : Application(), SingletonImageLoader.Factory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: RoutineRepository by lazy { RoutineRepository(this, database) }

    override fun onCreate() {
        super.onCreate()
        // Application.onCreate corre antes que cualquier servicio o receiver del proceso.
        TimerStore.init(this)
        appScope.launch { repository.seedIfNeeded() }
    }

    /** Cargador de imágenes global, con soporte para GIF animados. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
}