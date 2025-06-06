package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent.Child
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent.ChildConfiguration
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.article.ArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.features.cardset.CardSetDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.cardset_json_import.CardSetJsonImportDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_json_import.vm.CardSetJsonImportVM
import com.aglushkov.wordteacher.shared.features.cardsets.CardSetsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsRouter
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardVM
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.dict_configs.DictConfigsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.dict_configs.vm.DictConfigsVM
import com.aglushkov.wordteacher.shared.features.learning.LearningDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningRouter
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.features.learning.vm.SessionCardResult
import com.aglushkov.wordteacher.shared.features.learning_session_result.LearningSessionResultDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVM
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsRouter
import com.aglushkov.wordteacher.shared.features.webauth.WebAuthDecomposeComponent
import com.aglushkov.wordteacher.shared.features.webauth.vm.WebAuthRouter
import com.aglushkov.wordteacher.shared.features.webauth.vm.WebAuthVM
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import dev.icerock.moko.resources.desc.StringDesc
import io.ktor.util.reflect.instanceOf
import kotlinx.serialization.Serializable

interface MainDecomposeComponent:
    CardSetsRouter,
    ArticlesRouter,
    ArticleRouter,
    SettingsRouter,
    DashboardVM.Router,
    AuthOpener,
    SnackbarEventHolder {
    val childStack: Value<ChildStack<ChildConfiguration, Child>>
    val dialogsStateFlow: Value<ChildStack<ChildConfiguration, Child>>

    override fun openAddArticle(url: String?, showNeedToCreateCardSet: Boolean)
//    fun popDialog(inner: Any)
    fun popDialog(config: ChildConfiguration, onComplete: (topChild: MainDecomposeComponent.Child) -> Unit = {})
    override fun openArticle(state: ArticleVM.State)
    fun openCardSets()
    override fun openCardSet(state: CardSetVM.State)
    fun openCardSetInfo(state: CardSetInfoVM.State)
    override fun openLearning(state: LearningVM.State)
    fun openLearningSessionResult(results: List<SessionCardResult>)
    fun openDefinitions(word: String)
    fun back()
    fun popToRoot()
    override fun openWebAuth(networkType: SpaceAuthService.NetworkType)
    override fun onError(text: StringDesc)

    sealed class Child(
        val inner: Clearable?
    ): Clearable {
        data class Article(val vm: ArticleVM): Child(vm)
        data class CardSet(val vm: CardSetVM): Child(vm)
        data class CardSetInfo(val vm: CardSetInfoVM): Child(vm)
        data class CardSets(val vm: CardSetsVM): Child(vm)
        data class Learning(val vm: LearningVM): Child(vm)
        data class LearningSessionResult(val vm: LearningSessionResultVM): Child(vm)
        data class Tabs(val vm: TabDecomposeComponent): Child(vm)

        data class AddArticle(val vm: AddArticleDecomposeComponent): Child(vm)
        data class WebAuth(val vm: WebAuthVM): Child(vm)
        data class CardSetJsonImport(val vm: CardSetJsonImportVM): Child(vm)
        data class DictConfigs(val vm: DictConfigsVM): Child(vm)
        data class Definitions(val vm: DefinitionsVM): Child(vm)
        object EmptyDialog: Child(null)

        override fun onCleared() {
            inner?.onCleared()
        }
    }

    @Serializable
    sealed class ChildConfiguration {
        @Serializable data class CardSetInfoConfiguration(val state: CardSetInfoVM.State) : ChildConfiguration()
        @Serializable data class ArticleConfiguration(val state: ArticleVM.State) : ChildConfiguration()
        @Serializable data class CardSetConfiguration(val state: CardSetVM.State) : ChildConfiguration()
        @Serializable data class LearningConfiguration(val state: LearningVM.State) : ChildConfiguration()
        @Serializable data class LearningSessionResultConfiguration(val results: List<SessionCardResult>) : ChildConfiguration()
        @Serializable data class WebAuthConfiguration(val networkType: SpaceAuthService.NetworkType) : ChildConfiguration()
        @Serializable data object DictConfigs : ChildConfiguration()
        @Serializable data object CardSetJsonImportConfiguration : ChildConfiguration()
        @Serializable data object CardSetsConfiguration : ChildConfiguration()
        @Serializable data object TabsConfiguration : ChildConfiguration()
        @Serializable data class AddArticleConfiguration(val state: AddArticleVM.State = AddArticleVM.State()) : ChildConfiguration()
        @Serializable data class DefinitionConfiguration(val state: DefinitionsVM.State) : ChildConfiguration()
        @Serializable data object EmptyDialogConfiguration : ChildConfiguration() // TODO: it seems we can remove that
    }
}

class MainDecomposeComponentImpl(
    componentContext: ComponentContext,
    val childComponentFactory: (context: ComponentContext, configuration: MainDecomposeComponent.ChildConfiguration) -> Any
) : MainDecomposeComponent,
    ComponentContext by componentContext,
    SnackbarEventHolder by SnackbarEventHolderImpl() {

    init {
        snackbarEventRouter = object : SnackbarEventHolderRouter {
            override fun openArticle(id: Long) {
                this@MainDecomposeComponentImpl.openArticle(
                    ArticleVM.State(id = id)
                )
            }

            override fun openLocalCardSet(cardSetId: Long) {
                this@MainDecomposeComponentImpl.openCardSet(CardSetVM.State.LocalCardSet(id = cardSetId))
            }
        }
    }

    private val navigation = StackNavigation<MainDecomposeComponent.ChildConfiguration>()

    override val childStack: Value<ChildStack<MainDecomposeComponent.ChildConfiguration, MainDecomposeComponent.Child>> =
        childStack(
            source = navigation,
            serializer = MainDecomposeComponent.ChildConfiguration.serializer(), // Or null to disable navigation state saving
            initialConfiguration = MainDecomposeComponent.ChildConfiguration.TabsConfiguration,
            handleBackButton = true, // Pop the back stack on back button press
            childFactory = ::resolveChild,
        )

    private val dialogNavigation = StackNavigation<MainDecomposeComponent.ChildConfiguration>()
    override val dialogsStateFlow: Value<ChildStack<MainDecomposeComponent.ChildConfiguration, MainDecomposeComponent.Child>> =
        childStack(
            source = dialogNavigation,
            serializer = MainDecomposeComponent.ChildConfiguration.serializer(), // Or null to disable navigation state saving
            initialConfiguration = MainDecomposeComponent.ChildConfiguration.EmptyDialogConfiguration,
            key = "DialogKeyStack",
            handleBackButton = true, // Pop the back stack on back button press
            childFactory = ::resolveChild,
        )

    private fun resolveChild(
        configuration: MainDecomposeComponent.ChildConfiguration,
        componentContext: ComponentContext
    ): MainDecomposeComponent.Child = when (configuration) {
        is MainDecomposeComponent.ChildConfiguration.ArticleConfiguration ->
            MainDecomposeComponent.Child.Article(
                vm = childComponentFactory(componentContext, configuration) as ArticleDecomposeComponent
            )
        is MainDecomposeComponent.ChildConfiguration.CardSetConfiguration ->
            MainDecomposeComponent.Child.CardSet(
                vm = childComponentFactory(componentContext, configuration) as CardSetDecomposeComponent
            )
        is MainDecomposeComponent.ChildConfiguration.CardSetInfoConfiguration ->
            MainDecomposeComponent.Child.CardSetInfo(
                vm = childComponentFactory(componentContext, configuration) as CardSetInfoDecomposeComponent
            )
        is MainDecomposeComponent.ChildConfiguration.CardSetsConfiguration ->
            MainDecomposeComponent.Child.CardSets(
                vm = childComponentFactory(componentContext, configuration) as CardSetsDecomposeComponent
            )
        is MainDecomposeComponent.ChildConfiguration.TabsConfiguration ->
            MainDecomposeComponent.Child.Tabs(
                vm = childComponentFactory(componentContext, configuration) as TabDecomposeComponent
            )
        is MainDecomposeComponent.ChildConfiguration.AddArticleConfiguration -> MainDecomposeComponent.Child.AddArticle(
            vm = childComponentFactory(componentContext, configuration) as AddArticleDecomposeComponent
        )
        is MainDecomposeComponent.ChildConfiguration.DefinitionConfiguration -> MainDecomposeComponent.Child.Definitions(
            vm = childComponentFactory(componentContext, configuration) as DefinitionsDecomposeComponent
        )
        is MainDecomposeComponent.ChildConfiguration.LearningConfiguration -> MainDecomposeComponent.Child.Learning(
            vm = childComponentFactory(componentContext, configuration) as LearningDecomposeComponent
        )
        is MainDecomposeComponent.ChildConfiguration.LearningSessionResultConfiguration -> MainDecomposeComponent.Child.LearningSessionResult(
            vm = childComponentFactory(componentContext, configuration) as LearningSessionResultDecomposeComponent
        )
        is MainDecomposeComponent.ChildConfiguration.WebAuthConfiguration -> MainDecomposeComponent.Child.WebAuth(
            vm = (childComponentFactory(componentContext, configuration) as WebAuthDecomposeComponent).apply {
                router = object : WebAuthRouter {
                    override fun onCompleted(result: AuthOpener.AuthResult) {
                        authListeners.onEach { it.onCompleted(result) }
                    }

                    override fun onError(throwable: Throwable) {
                        authListeners.onEach { it.onError(throwable) }
                    }
                }
            }
        )
        is MainDecomposeComponent.ChildConfiguration.CardSetJsonImportConfiguration -> MainDecomposeComponent.Child.CardSetJsonImport(
            vm = childComponentFactory(componentContext, configuration) as CardSetJsonImportDecomposeComponent
        )
        is MainDecomposeComponent.ChildConfiguration.DictConfigs -> MainDecomposeComponent.Child.DictConfigs(
            vm = childComponentFactory(componentContext, configuration) as DictConfigsDecomposeComponent
        )
        is MainDecomposeComponent.ChildConfiguration.EmptyDialogConfiguration -> MainDecomposeComponent.Child.EmptyDialog

    }

    override fun openArticle(state: ArticleVM.State) =
        navigation.pushChildConfigurationIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.ArticleConfiguration(state)
        )

    override fun openCardSet(state: CardSetVM.State) {
        navigation.pushChildConfigurationIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.CardSetConfiguration(state)
        )
    }

    override fun openCardSetInfo(state: CardSetInfoVM.State) {
        navigation.pushChildConfigurationIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.CardSetInfoConfiguration(state)
        )
    }

    override fun openCardSets() {
        navigation.pushChildConfigurationIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.CardSetsConfiguration
        )
    }

    override fun openLearning(state: LearningVM.State) {
        addDialogConfigIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.LearningConfiguration(state)
        )
    }

    override fun openWebAuth(networkType: SpaceAuthService.NetworkType) {
        addDialogConfigIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.WebAuthConfiguration(networkType)
        )
    }

    override fun onError(text: StringDesc) {
        onError(text, null, null)
    }

    override fun openDictConfigs() {
        navigation.pushChildConfigurationIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.DictConfigs
        )
    }

    override fun openLearningSessionResult(results: List<SessionCardResult>) {
        addDialogConfigIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.LearningSessionResultConfiguration(results)
        )
    }

    override fun openJsonImport() {
        addDialogConfigIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.CardSetJsonImportConfiguration
        )
    }

    override fun back() = navigation.popIfNotEmpty()
    override fun popToRoot() {
        closeDialogs()
        navigation.popToRoot()

    }

    // Dialogs

    override fun openAddArticle(url: String?, showNeedToCreateCardSet: Boolean) {
        addDialogConfigIfNotAtTop(MainDecomposeComponent.ChildConfiguration.AddArticleConfiguration(
            state = AddArticleVM.State(uri = url, needToCreateSet = showNeedToCreateCardSet, showNeedToCreateCardSet = showNeedToCreateCardSet)
        ))
    }

    override fun openAddArticle() {
        openAddArticle(null, true)
    }

    override fun openDefinitions(word: String) {
        addDialogConfigIfNotAtTop(MainDecomposeComponent.ChildConfiguration.DefinitionConfiguration(
            state = DefinitionsVM.State(word = word)
        ))
    }

    private inline fun <reified C: MainDecomposeComponent.ChildConfiguration> addDialogConfigIfNotAtTop(config: C) {
        dialogNavigation.pushNew(config)
    }

    override fun popDialog(config: MainDecomposeComponent.ChildConfiguration, onComplete: (topChild: MainDecomposeComponent.Child) -> Unit) {
        dialogNavigation.navigate ({ configs ->
            configs.filter { !it.instanceOf(config::class) }
        }) { n, o ->
            val activeChild = if (dialogsStateFlow.active.instance !is MainDecomposeComponent.Child.EmptyDialog) {
                dialogsStateFlow.active.instance
            } else {
                childStack.active.instance
            }

            onComplete(activeChild)
        }
    }

    private fun closeDialogs() {
        dialogNavigation.popToRoot()
    }

    override fun closeArticle() {
        back()
    }

    // AuthOpener

    private val authListeners = mutableListOf<AuthOpener.Listener>()

    override fun addAuthListener(listener: AuthOpener.Listener) {
        authListeners.add(listener)
    }

    override fun removeAuthListener(listener: AuthOpener.Listener) {
        authListeners.remove(listener)
    }
}
