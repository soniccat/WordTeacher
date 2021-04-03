package com.aglushkov.wordteacher.di

import android.content.Context
import android.text.Annotation
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.article.blueprints.ParagraphBlueprint
import com.aglushkov.wordteacher.androidApp.features.article.blueprints.ROUNDED_ANNOTATION_VALUE_ADJECTIVE
import com.aglushkov.wordteacher.androidApp.features.article.blueprints.ROUNDED_ANNOTATION_VALUE_ADVERB
import com.aglushkov.wordteacher.androidApp.features.article.blueprints.ROUNDED_ANNOTATION_VALUE_PHRASE
import com.aglushkov.wordteacher.androidApp.features.article.blueprints.RoundedBgAnnotations
import com.aglushkov.wordteacher.androidApp.features.article.di.ViewContext
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsVMWrapper
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
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
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
        definitionsVM: DefinitionsVMWrapper,
        routerResolver: RouterResolver,
        articlesRepository: ArticleRepository,
        state: ArticleVM.State,
        idGenerator: IdGenerator
    ): ArticleVM {
        return ArticleVMImpl(
            definitionsVM.vm,
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
        singleRenderers: Map<String, SingleLineRenderer>,
        multilineRenderers: Map<String, MultiLineRenderer>
    ): BgRendererResolver {
        return object : BgRendererResolver {
            override fun resolve(annotation: Annotation, isSingleLine: Boolean): TextRoundedBgRenderer? {
                return if (isSingleLine) {
                    singleRenderers[annotation.value]
                } else {
                    multilineRenderers[annotation.value]
                }
            }
        }
    }

    @FragmentComp @Provides @IntoMap @StringKey(ROUNDED_ANNOTATION_VALUE_ADJECTIVE)
    fun adjectiveSingleRenderer(@ViewContext context: Context) = SingleLineRenderer(context, R.style.AdjectiveBgStyle)

    @FragmentComp @Provides @IntoMap @StringKey(ROUNDED_ANNOTATION_VALUE_ADJECTIVE)
    fun adjectiveMultilineRenderer(@ViewContext context: Context) = MultiLineRenderer(context, R.style.AdjectiveBgStyle)

    @FragmentComp @Provides @IntoMap @StringKey(ROUNDED_ANNOTATION_VALUE_ADVERB)
    fun adverbSingleRenderer(@ViewContext context: Context) = SingleLineRenderer(context, R.style.AdverbBgStyle)

    @FragmentComp @Provides @IntoMap @StringKey(ROUNDED_ANNOTATION_VALUE_ADVERB)
    fun adverbMultilineRenderer(@ViewContext context: Context) = MultiLineRenderer(context, R.style.AdverbBgStyle)

    @FragmentComp @Provides @IntoMap @StringKey(ROUNDED_ANNOTATION_VALUE_PHRASE)
    fun phraseSingleRenderer(@ViewContext context: Context) = SingleLineRenderer(context, R.style.PhraseBgStyle)

    @FragmentComp @Provides @IntoMap @StringKey(ROUNDED_ANNOTATION_VALUE_PHRASE)
    fun phraseMultilineRenderer(@ViewContext context: Context) = MultiLineRenderer(context, R.style.PhraseBgStyle)
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ArticleBinder
