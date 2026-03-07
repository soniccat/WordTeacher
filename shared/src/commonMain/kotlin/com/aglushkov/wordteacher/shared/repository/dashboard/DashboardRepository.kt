package com.aglushkov.wordteacher.shared.repository.dashboard

import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.serialization.SerializableFileCache
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.service.SpaceDashboardResponse
import com.aglushkov.wordteacher.shared.service.SpaceDashboardService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path
import kotlin.let
import kotlin.reflect.typeOf
import kotlin.time.Duration
import kotlin.time.Instant

class DashboardRepository(
    private val spaceDashboardService: SpaceDashboardService,
    private val timeSource: TimeSource,
    cacheFilePath: Path,
    fileSystem: FileSystem,
): SimpleResourceRepository<SpaceDashboardResponse, Unit>(
    needPreload = true
) {
    private var loadDate: Instant? = null

    private val responseCache = SerializableFileCache<SpaceDashboardResponse>(
        kType = typeOf<SpaceDashboardResponse>(),
        filePath = cacheFilePath,
        fileSystem = fileSystem,
    )

    override suspend fun preload(arg: Unit): SpaceDashboardResponse? {
        return responseCache.stateFlow.waitUntilDone().data()
    }

    suspend fun reloadIfNeeded() {
        loadDate?.let { loadDate ->
            if (timeSource.timeInstant().minus(loadDate).inWholeMinutes >= 5) {
                if (!stateFlow.value.isLoading()) {
                    load(Unit)
                }
            }
        }
    }

    override suspend fun load(arg: Unit): SpaceDashboardResponse {
        return spaceDashboardService.load().toOkResponse()
            .also {
                responseCache.set(it)
                loadDate = timeSource.timeInstant()
            }
    }
}
