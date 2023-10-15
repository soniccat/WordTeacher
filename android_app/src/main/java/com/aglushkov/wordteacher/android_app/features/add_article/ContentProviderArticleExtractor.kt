package com.aglushkov.wordteacher.android_app.features.add_article

import android.net.Uri
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContent
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContentExtractor
import com.aglushkov.wordteacher.shared.features.add_article.vm.toArticleContentExtractor

fun ContentProviderRepository.toArticleContentExtractor(): ArticleContentExtractor {
    return this.toArticleContentExtractor(
        canExtract = {
            try {
                Uri.parse(it).scheme == "content"
            } catch (t: Exception) {
                false
            }
        },
        transformer = { content ->
            ArticleContent(
                text = content
            )
        }
    )
}
