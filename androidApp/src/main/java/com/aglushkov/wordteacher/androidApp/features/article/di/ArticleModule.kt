package com.aglushkov.wordteacher.di

import android.content.Context
import android.text.Annotation
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.article.blueprints.ParagraphBlueprint
import com.aglushkov.wordteacher.androidApp.features.article.blueprints.RoundedBgAnnotations
import com.aglushkov.wordteacher.androidApp.features.article.di.ViewContext
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.androidApp.general.textroundedbg.BgRendererResolver
import com.aglushkov.wordteacher.androidApp.general.textroundedbg.MultiLineRenderer
import com.aglushkov.wordteacher.androidApp.general.textroundedbg.SingleLineRenderer
import com.aglushkov.wordteacher.androidApp.general.textroundedbg.TextRoundedBgRenderer
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVMImpl
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier

@Module
class ArticleModule {

    @FragmentComp
    @Provides
    @ArticleBinder
    fun createItemViewBinder(
        paragraphBlueprint: ParagraphBlueprint,
    ) = ViewItemBinder()
        .addBlueprint(paragraphBlueprint)

    @FragmentComp
    @Provides
    fun viewModel(
        definitionsVM: DefinitionsVM,
        routerResolver: RouterResolver,
        articlesRepository: ArticleRepository,
        state: ArticleVM.State,
        idGenerator: IdGenerator
    ): ArticleVM {
        return ArticleVMImpl(
            definitionsVM,
            articlesRepository,
            state,
            object : ArticleRouter {
                override fun closeArticle() {
                    routerResolver.router?.get()?.closeArticle()
                }
            },
            idGenerator
        )
    }

    @FragmentComp
    @Provides
    fun articleRepository(
        database: AppDatabase,
        nlpCore: NLPCore
    ) = ArticleRepository(database, nlpCore)

    @FragmentComp
    @Provides
    fun bgRendererResolver(
        @ViewContext context: Context
    ): BgRendererResolver {
        return object : BgRendererResolver {
            override fun resolve(annotation: Annotation, isSingleLine: Boolean): TextRoundedBgRenderer? {
                // TODO: cache renderers using DI...
                return when (annotation.value) {
                    RoundedBgAnnotations.Adjective.annotation.value -> {
                        if (isSingleLine) {
                            SingleLineRenderer(context, R.style.AdjectiveBgStyle)
                        } else {
                            MultiLineRenderer(context, R.style.AdjectiveBgStyle)
                        }
                    }
                    RoundedBgAnnotations.Adverb.annotation.value -> {
                        if (isSingleLine) {
                            SingleLineRenderer(context, R.style.AdverbBgStyle)
                        } else {
                            MultiLineRenderer(context, R.style.AdverbBgStyle)
                        }
                    }
                    else -> null
                }
            }
        }
    }
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ArticleBinder
