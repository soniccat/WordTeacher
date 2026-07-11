package com.aglushkov.wordteacher.shared.repository.dashboard

import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.collectUntilDone
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.serialization.SerializableFileCache
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.service.SpaceDashboardResponse
import com.aglushkov.wordteacher.shared.service.SpaceDashboardService
import okio.FileSystem
import okio.Path
import kotlin.reflect.typeOf
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
        return responseCache.stateFlow.collectUntilDone().data()
    }

    suspend fun reloadIfNeeded() {
        val safeLoadDate = loadDate ?: return
        if (timeSource.timeInstant().minus(safeLoadDate).inWholeMinutes >= 15) {
            if (!stateFlow.value.isLoading()) {
                load(Unit).collectUntilDone()
            }
        }
    }

    override suspend fun loadInternal(arg: Unit): SpaceDashboardResponse {
        return spaceDashboardService.load().toOkResponse()
            .also {
                responseCache.set(it)
                loadDate = timeSource.timeInstant()
            }
    }
}
