package com.aglushkov.wordteacher.androidApp.features.articles.views

import com.aglushkov.resources.desc.Raw
import com.aglushkov.resources.desc.StringDesc
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

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc {
        return StringDesc.Raw("Error Text")
    }

    override fun onTryAgainClicked() {}
}