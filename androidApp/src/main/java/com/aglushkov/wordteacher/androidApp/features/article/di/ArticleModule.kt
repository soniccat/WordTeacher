package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.article.blueprints.ParagraphBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.DefinitionsDisplayModeBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordDefinitionBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordDividerBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordExampleBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordHeaderBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordPartOfSpeechBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordSubHeaderBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordSynonymBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordTitleBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordTranscriptionBlueprint
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVMImpl
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
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
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ArticleBinder
