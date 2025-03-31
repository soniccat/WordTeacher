package com.aglushkov.wordteacher.shared.tasks

import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect

data class ArticleSample(
    val title: String,
    val text: String,
    val link: String?,
)

class AddArticleSampleTask(
    private val articlesRepository: ArticlesRepository,
    private val settings: FlowSettings,
    private val articleProvider: () -> ArticleSample
): Task {
    override suspend fun run(nextTasksChannel: Channel<Task>) {
        val isImported = settings.getBoolean(IS_ARTICLE_SAMPLE_IMPORTED_KEY, false)
        if (isImported) {
            return
        }

        val article = articleProvider()
        articlesRepository.createArticle(
            article.title,
            article.text,
            article.link,
        ).collect()

        settings.putBoolean(IS_ARTICLE_SAMPLE_IMPORTED_KEY, true)
    }
}

private const val IS_ARTICLE_SAMPLE_IMPORTED_KEY = "IS_ARTICLE_SAMPLE_IMPORTED_KEY"