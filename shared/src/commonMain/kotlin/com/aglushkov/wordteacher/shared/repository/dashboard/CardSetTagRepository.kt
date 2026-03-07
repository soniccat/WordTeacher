package com.aglushkov.wordteacher.shared.repository.dashboard

import kotlinx.coroutines.flow.map

class CardSetTagRepository(
    dashboardRepository: DashboardRepository
) {
    val cardSetTags = dashboardRepository.stateFlow.map {
        it.data()?.tagWithCardSetsBlock?.tagWithCardSets?.map { it.tag }
            .orEmpty()
    }
}
