package com.aglushkov.wordteacher.shared.features.articles.vm

import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ArticlesVM(
    articlesRepository: ArticlesRepository,
    private val idGenerator: IdGenerator,
    private val router: ArticlesRouter
): ViewModel() {

    private val articlesFlow = articlesRepository.shortArticles
    val articles = MutableStateFlow<Resource<List<BaseViewItem<*>>>>(Resource.Uninitialized())

    init {
        viewModelScope.launch {
            articlesFlow.map {
                Logger.v("build view items")
                it.copyWith(buildViewItems(it.data() ?: emptyList()))
            }.forward(articles)
        }
    }

    fun onCreateTextArticleClicked() {
        router.openAddArticle()
    }

    private fun buildViewItems(articles: List<ShortArticle>): List<BaseViewItem<*>> {
        val items = mutableListOf<BaseViewItem<*>>()
        articles.forEach {
            val dateTime = Instant.fromEpochMilliseconds(it.date).toLocalDateTime(TimeZone.currentSystemDefault())
            val dateTimeString = "${dateTime.dayOfMonth}.${dateTime.monthNumber}.${dateTime.year}"

            items.add(ArticleViewItem(it.name, dateTimeString).apply {
                id = idGenerator.nextId()
            })
        }

        return items
    }
}