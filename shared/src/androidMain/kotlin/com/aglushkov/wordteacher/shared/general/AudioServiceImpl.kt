package com.aglushkov.wordteacher.shared.general

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

@UnstableApi
class AudioServiceImpl(
    val context: Context,
): AudioService {
    private val evictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024)
    private val simpleCache = SimpleCache(
        File(context.cacheDir, "media"),
        evictor,
        StandaloneDatabaseProvider(context)
    )
    val player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(
                    CacheDataSourceFactory(
                        context,
                        simpleCache,
                        5 * 1024 * 1024
                    )
                )
        ).build()

    override fun play(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }
}

@UnstableApi
class CacheDataSourceFactory(
    private val context: Context,
    private val cache: Cache,
    private val maxFileSize: Long
) : DataSource.Factory {
    private val defaultDatasourceFactory = DefaultDataSource.Factory(
        this.context,
        DefaultHttpDataSource.Factory()
    )

    override fun createDataSource(): DataSource {
        return CacheDataSource(
            cache,
            defaultDatasourceFactory.createDataSource(),
            FileDataSource(),
            CacheDataSink(cache, maxFileSize),
            CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
            null
        )
    }
}