package com.aglushkov.wordteacher.android_app.features.articles.views

import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.flow.MutableStateFlow

class ArticlesVMPreview(
    articles: Resource<List<BaseViewItem<*>>>
): ArticlesVM {
    override val articles = MutableStateFlow(articles)

    override fun onCreateTextArticleClicked() {}

    override fun onArticleClicked(item: ArticleViewItem) {}

    override fun onArticleRemoved(item: ArticleViewItem) {}

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc {
        return StringDesc.Raw("Error Text")
    }

    override fun onTryAgainClicked() {}
}