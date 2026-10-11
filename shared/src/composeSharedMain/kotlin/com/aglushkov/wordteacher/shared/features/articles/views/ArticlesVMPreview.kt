package com.aglushkov.wordteacher.shared.features.articles.views

import org.jetbrains.compose.resources.StringResource
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import com.aglushkov.wordteacher.shared.general.settings.HintType
import kotlinx.coroutines.flow.MutableStateFlow

class ArticlesVMPreview(
    articles: Resource<List<BaseViewItem<*>>>
): ArticlesVM {
    override var router: ArticlesRouter? = null

    override val articles = MutableStateFlow(articles)

    override fun onCreateTextArticleClicked() {}

    override fun onArticleClicked(item: ArticleViewItem) {}

    override fun onArticleRemoved(item: ArticleViewItem) {}

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringResourceWrapper {
        return StringResourceWrapper.Raw("Error Text")
    }

    override fun onTryAgainClicked() {}

    override fun onHintClicked(hintType: HintType) {}
}