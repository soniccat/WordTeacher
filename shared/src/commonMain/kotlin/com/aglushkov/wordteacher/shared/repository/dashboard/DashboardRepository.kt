package com.aglushkov.wordteacher.shared.repository.dashboard

import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.serialization.SerializableFileCache
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.service.SpaceDashboardResponse
import com.aglushkov.wordteacher.shared.service.SpaceDashboardService
import okio.FileSystem
import okio.Path
import kotlin.reflect.typeOf

class DashboardRepository(
    private val spaceDashboardService: SpaceDashboardService,
    cacheFilePath: Path,
    fileSystem: FileSystem,
): SimpleResourceRepository<SpaceDashboardResponse, Unit>() {
    private val responseCache = SerializableFileCache<SpaceDashboardResponse>(
        kType = typeOf<SpaceDashboardResponse>(),
        filePath = cacheFilePath,
        fileSystem = fileSystem,
    )

    override suspend fun preload(arg: Unit): SpaceDashboardResponse? {
        return responseCache.stateFlow.waitUntilDone().data()
    }

    override suspend fun load(arg: Unit): SpaceDashboardResponse {
        return spaceDashboardService.load().toOkResponse()
            .also {
                responseCache.set(it)
            }
    }
}
