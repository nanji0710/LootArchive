package com.nanji.lootarchive

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.nanji.lootarchive.util.NotificationUtil
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LootArchiveApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.05).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("coil_cache")).maxSizeBytes(30 * 1024 * 1024).build() }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        NotificationUtil.createNotificationChannels(this)
    }
}
