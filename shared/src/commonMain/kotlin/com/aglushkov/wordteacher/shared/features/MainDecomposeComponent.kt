package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.ArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.cardset.CardSetDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardsets.CardSetsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsRouter
import com.aglushkov.wordteacher.shared.features.learning.LearningDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.features.learning.vm.SessionCardResult
import com.aglushkov.wordteacher.shared.features.learning_session_result.LearningSessionResultDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVM
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsRouter
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.RouterStateChangeHandler
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.popIfNotEmpty
import com.aglushkov.wordteacher.shared.general.pushChildConfigurationIfNotAtTop
import com.aglushkov.wordteacher.shared.general.toClearables
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.Router
import com.arkivanov.decompose.router.RouterState
import com.arkivanov.decompose.router.router
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlin.properties.Delegates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface MainDecomposeComponent: DefinitionsRouter, SettingsRouter {
    val routerState: Value<RouterState<*, Child>>
    val dialogsStateFlow: StateFlow<List<com.arkivanov.decompose.Child.Created<*, Child>>>

    fun openAddArticleDialog()
    fun popDialog(inner: Any)
    fun popDialog(child: Child)
    fun openArticle(id: Long)
    fun openCardSet(id: Long)
    override fun openCardSets()
    fun openLearning(ids: List<Long>)
    fun openLearningSessionResult(results: List<SessionCardResult>)
    override fun openGoogleAuth()
    fun back()

    sealed class Child(
        val inner: Clearable?
    ): Clearable {
        data class Article(val vm: ArticleVM): Child(vm)
        data class CardSet(val vm: CardSetVM): Child(vm)
        data class CardSets(val vm: CardSetsVM): Child(vm)
        data class Learning(val vm: LearningVM): Child(vm)
        data class LearningSessionResult(val vm: LearningSessionResultVM): Child(vm)
        data class Tabs(val vm: TabDecomposeComponent): Child(vm)

        data class AddArticle(val vm: AddArticleDecomposeComponent): Child(vm)
        object EmptyDialog: Child(null)

        override fun onCleared() {
            inner?.onCleared()
        }
    }

    sealed class ChildConfiguration: Parcelable {
        @Parcelize data class ArticleConfiguration(val id: Long) : ChildConfiguration()
        @Parcelize data class CardSetConfiguration(val id: Long) : ChildConfiguration()
        @Parcelize data class LearningConfiguration(val ids: List<Long>) : ChildConfiguration()
        @Parcelize data class LearningSessionResultConfiguration(val results: List<SessionCardResult>) : ChildConfiguration()
        @Parcelize object CardSetsConfiguration : ChildConfiguration()
        @Parcelize object TabsConfiguration : ChildConfiguration()

        @Parcelize object AddArticleConfiguration : ChildConfiguration()
        @Parcelize object EmptyDialogConfiguration : ChildConfiguration()
    }
}

class MainDecomposeComponentImpl(
    componentContext: ComponentContext,
    val childComponentFactory: (context: ComponentContext, configuration: MainDecomposeComponent.ChildConfiguration) -> Any
) : MainDecomposeComponent, ComponentContext by componentContext {

    private val router: Router<MainDecomposeComponent.ChildConfiguration, MainDecomposeComponent.Child> =
        router(
            initialConfiguration = MainDecomposeComponent.ChildConfiguration.TabsConfiguration,
            key = "MainRouter",
            handleBackButton = true,
            childFactory = ::resolveChild
        )

    private val routerStateChangeHandler = RouterStateChangeHandler()
    override val routerState: Value<RouterState<*, MainDecomposeComponent.Child>> = router.state.map {
        routerStateChangeHandler.onClearableChanged(it.toClearables())
        it
    }

    private val dialogStateChangeHandler = RouterStateChangeHandler()
    override val dialogsStateFlow = MutableStateFlow<List<Child.Created<*, MainDecomposeComponent.Child>>>(emptyList())
    private var dialogHolders: List<DialogHolder> by Delegates.observable(emptyList()) { _, _, newValue ->
        dialogStateChangeHandler.onClearableChanged(newValue.map { it.child.instance })
        dialogsStateFlow.value = newValue.map { it.child }
    }

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
        is MainDecomposeComponent.ChildConfiguration.LearningConfiguration -> MainDecomposeComponent.Child.Learning(
            vm = childComponentFactory(componentContext, configuration) as LearningDecomposeComponent
        )
        is MainDecomposeComponent.ChildConfiguration.LearningSessionResultConfiguration -> MainDecomposeComponent.Child.LearningSessionResult(
            vm = childComponentFactory(componentContext, configuration) as LearningSessionResultDecomposeComponent
        )
        is MainDecomposeComponent.ChildConfiguration.EmptyDialogConfiguration -> MainDecomposeComponent.Child.EmptyDialog

    }

    override fun openArticle(id: Long) =
        router.pushChildConfigurationIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.ArticleConfiguration(id)
        )

    override fun openCardSet(id: Long) {
        router.pushChildConfigurationIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.CardSetConfiguration(id)
        )
    }

    override fun openCardSets() {
        router.pushChildConfigurationIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.CardSetsConfiguration
        )
    }

    override fun openLearning(ids: List<Long>) {
        addDialogConfigIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.LearningConfiguration(ids)
        )
    }

    override fun openLearningSessionResult(results: List<SessionCardResult>) {
        addDialogConfigIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.LearningSessionResultConfiguration(results)
        )
    }

    override fun openGoogleAuth() {
        TODO("Open google auth")
    }

    override fun back() = router.popIfNotEmpty()

    // Dialogs

    override fun openAddArticleDialog() {
        addDialogConfigIfNotAtTop(MainDecomposeComponent.ChildConfiguration.AddArticleConfiguration)
    }

    private inline fun <reified C: MainDecomposeComponent.ChildConfiguration> addDialogConfigIfNotAtTop(config: C) {
        if (dialogHolders.lastOrNull()?.child?.configuration is C) {
            return
        }

        dialogHolders = dialogHolders + dialogHolder(config)
    }

    override fun popDialog(inner: Any) {
        findChildByInner(inner)?.let { child ->
            popDialog(child)
        }
    }

    override fun popDialog(child: MainDecomposeComponent.Child) {
        val index = dialogHolders.indexOfLast { it.child.instance == child }
        if (index != -1) {
            val holder = dialogHolders[index]
            holder.lifecycle.destroy()
            dialogHolders = dialogHolders.filterIndexed { i, dialogHolder -> i != index }
        }
    }

    private fun findChildByInner(inner: Any): Child<*, *>? {
        return dialogHolders.lastOrNull { it.child.instance.inner == inner }?.child
    }

    private fun dialogHolder(config: MainDecomposeComponent.ChildConfiguration): DialogHolder {
        val lifecycle = LifecycleRegistry() // An instance of LifecycleRegistry associated with the new child
        val childContext = childContext(key = config::class.toString() , lifecycle = lifecycle)
        val child = Child.Created(configuration = config, instance = resolveChild(config, childContext))
        lifecycle.resume()

        return DialogHolder(
            child = child,
            lifecycle = lifecycle,
        )
    }

    private class DialogHolder(
        val child: Child.Created<MainDecomposeComponent.ChildConfiguration, MainDecomposeComponent.Child>,
        val lifecycle: LifecycleRegistry,
    )
}
