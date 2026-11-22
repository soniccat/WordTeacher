package com.aglushkov.wordteacher.shared.features.dict_configs.vm

import com.aglushkov.wordteacher.shared.apiproviders.yandex.service.YandexService.Companion.Lookup
import com.aglushkov.wordteacher.shared.apiproviders.yandex.service.YandexService.Companion.LookupFlags
import com.aglushkov.wordteacher.shared.apiproviders.yandex.service.YandexService.Companion.LookupLang
import com.aglushkov.wordteacher.shared.apiproviders.yandex.service.YandexService.Companion.LookupLangDefault
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDict
import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.dsl.DslDict
import com.aglushkov.wordteacher.shared.dicts.wordlist.WordListDict
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.on
import com.aglushkov.wordteacher.shared.general.resource.onError
import com.aglushkov.wordteacher.shared.general.toStringResource
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.config.ConfigConnectParams
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.remote_dict.RemoteDictRepository
import com.aglushkov.wordteacher.shared.usecase.dsl_dict_import_use_case.DslDictImportUseCase
import wordteacher.shared.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import wordteacher.shared.generated.resources.dictconfigs_offline_section_title
import wordteacher.shared.generated.resources.dictconfigs_online_section_title

// https://yandex.com/dev/dictionary/doc/dg/reference/lookup.html
data class YandexSettings(
    val applyFamilySearchFilter: Boolean = false,
    val enableSearchingByWordForm: Boolean = false,
    val enableFilterThatRequiresMatchingPartsOfSpeechForSearchWordAndTranslation: Boolean = false,
) {
    companion object {
        fun fromInt(v: Int): YandexSettings {
            return YandexSettings(
                applyFamilySearchFilter = (v and 1) == 1,
                enableSearchingByWordForm = (v and 4) == 4,
                enableFilterThatRequiresMatchingPartsOfSpeechForSearchWordAndTranslation = (v and 8) == 8,
            )
        }
    }

    fun toInt(): Int {
        var r = 0
        if (applyFamilySearchFilter) {
            r = r or 1
        }
        if (enableSearchingByWordForm) {
            r = r or 4
        }
        if (enableFilterThatRequiresMatchingPartsOfSpeechForSearchWordAndTranslation) {
            r = r or 8
        }
        return r
    }
}

interface DictConfigsRouter {
    fun onError(text: StringResourceWrapper)
}

interface DictConfigsVM: Clearable {
    var router: DictConfigsRouter?
    val viewItems: StateFlow<List<BaseViewItem<*>>>

    fun onConfigDeleteClicked(item: ConfigYandexViewItem)
    fun onYandexConfigChanged(item: ConfigYandexViewItem, key: String?, lang: String?, settings: YandexSettings?)
    fun onConfigAddClicked(type: ConfigCreateViewItem.Type)
    fun onDictDeleted(item: ConfigDictViewItem)
    fun onRemoteDictStartLoadingClicked(item: RemoteDictViewItem)
}

open class DictConfigsVMImpl(
    private val configRepository: ConfigRepository,
    private val dslDictOpenController: FileOpenController,
    private val dictRepository: DictRepository,
    private val remoteDictRepository: RemoteDictRepository,
    private val idGenerator: IdGenerator,
    private val analytics: Analytics,
    private val dslDictImportUseCase: DslDictImportUseCase,
): ViewModel(), DictConfigsVM {
    data class WordTeacherDictExt(
        val remoteDict: WordTeacherDict,
        val loadingProgress: Float,
        val sizeText: String,
        val isNewVersion: Boolean = false
    )

    override var router: DictConfigsRouter? = null
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override val viewItems = combine(
        configRepository.flow,
        combine(
            dictRepository.dicts,
            remoteDictRepository.remoteDictWithProgress
        ) { dicts, remoteDicts ->
            if (dicts.isLoaded() && remoteDicts.isLoaded()) {
                // filter remote dicts by already loaded ones
                dicts to remoteDicts.mapLoadedData {
                    it.mapNotNull { remoteDict ->
                        // look for already downloaded version by name
                        val loadedDictVersion = dicts.data().orEmpty().firstOrNull { dict ->
                            val dslDict = dict as? DslDict ?: return@firstOrNull false
                            dslDict.name == remoteDict.first.name
                        } as? DslDict

                        // check if online version is newer
                        if (loadedDictVersion != null) {
                            val modificationDate = loadedDictVersion.modificationDate ?: return@mapNotNull null
                            if (modificationDate > remoteDict.first.modificationDate) {
                                null
                            } else {
                                WordTeacherDictExt(
                                    remoteDict = remoteDict.first,
                                    loadingProgress = remoteDict.second,
                                    sizeText = remoteDict.first.size.formatBytes(),
                                    isNewVersion = true
                                )
                            }
                        } else {
                            WordTeacherDictExt(
                                remoteDict = remoteDict.first,
                                loadingProgress = remoteDict.second,
                                sizeText = remoteDict.first.size.formatBytes(),
                                isNewVersion = false
                            )
                        }
                    }
                }
            } else if (dicts.isLoading() || remoteDicts.isLoading()) {
                dicts to remoteDicts.mapLoadedData { emptyList() }
            } else {
                dicts to remoteDicts.mapLoadedData {
                    it.map { (remoteDict, loadingProgress) ->
                        WordTeacherDictExt(
                            remoteDict = remoteDict,
                            loadingProgress = loadingProgress,
                            sizeText = remoteDict.size.formatBytes(),
                        )
                    }
                }
            }
        },
        dslDictOpenController.state,
        dslDictImportUseCase.loadingPaths,
    ) { configs, (dicts, remoteDicts), dictImportProgress, _ ->
        buildViewItems(configs, dicts, remoteDicts, dictImportProgress.isLoading())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    init {
        viewModelScope.launch {
            remoteDictRepository.loadIfNotLoaded(Unit).collect()
        }
    }

    override fun onConfigDeleteClicked(item: ConfigYandexViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("DictConfigs.configDeleteClicked"))
        yandexConfig(item)?.let {
            configRepository.removeConfig(it.id)
        }
    }

    private fun yandexConfig(item: ConfigYandexViewItem) =
        configRepository.value.data()
            ?.firstOrNull { it.id == item.id.toInt() }

    private fun buildViewItems(
        configs: Resource<List<Config>>,
        dicts: Resource<List<Dict>>,
        remoteDicts: Resource<List<WordTeacherDictExt>>,
        isDictImporting: Boolean,
    ): List<BaseViewItem<*>> = buildList<BaseViewItem<*>> {
        add(ConfigHeaderViewItem(StringResourceWrapper.String(Res.string.dictconfigs_offline_section_title)))
        remoteDicts.on(
            data = {
                it.onEach { remoteDict ->
                    add(
                        RemoteDictViewItem(
                            remoteDict.remoteDict.name,
                            remoteDict.loadingProgress.let { progress ->
                                if (dslDictImportUseCase.isPathInProgress(
                                        remoteDictRepository.loadedDictPath(remoteDict.remoteDict)
                                    )) {
                                    RemoteDictViewItem.IS_IMPORTING
                                } else if (progress == -1.0f) {
                                    RemoteDictViewItem.NOT_LOADING
                                } else {
                                    progress
                                }
                            },
                            remoteDict.remoteDict,
                            sizeText = remoteDict.sizeText,
                            remoteDict.isNewVersion,
                        )
                    )
                }
            },
            error = {
                // TODO:
            }
        )
        dicts.on(
            data = {
                it.onEach { dict ->
                    if (dict !is WordListDict) {
                        val wordCount = if (dict is DslDict) {
                            dict.wordCount
                        } else {
                            null
                        }
                        add(ConfigDictViewItem(dict.name, dict.path, wordCount))
                    }
                }
            },
            breakOnDataCall = false,
            error = {
                add(ConfigTextViewItem(it.toStringResource()))
            }
        )
        if (remoteDicts.isLoading() || dicts.isLoading() || isDictImporting) {
            add(ConfigLoadingViewItem())
        }
        add(ConfigCreateViewItem(ConfigCreateViewItem.Type.Offline))

        // yandex api
        add(ConfigHeaderViewItem(StringResourceWrapper.String(Res.string.dictconfigs_online_section_title)))
        configs.on(
            loaded = {
                it.onEach { config ->
                    if (config.type == Config.Type.Yandex) {
                        val flag = config.methods[Lookup]?.get(LookupFlags)?.toIntOrNull() ?: 0
                        add(
                            ConfigYandexViewItem(
                                id = config.id.toLong(),
                                hasToken = config.connectParams.securedKey.isNotEmpty(),
                                lang = config.methods[Lookup]?.get(LookupLang) ?: LookupLangDefault,
                                settings = YandexSettings.fromInt(flag)
                            )
                        )
                    }
                }
            },
            loading = {
                add(ConfigLoadingViewItem())
            },
            error = {
                add(ConfigTextViewItem(it.toStringResource()))
            }
        )

        add(ConfigCreateViewItem(ConfigCreateViewItem.Type.Online))
    }.let {
        generateViewItemIds(it, viewItems.value, idGenerator)
    }

    override fun onYandexConfigChanged(item: ConfigYandexViewItem, key: String?, lang: String?, settings: YandexSettings?) {
        analytics.send(AnalyticEvent.createActionEvent("DictConfigs.yandexConfigChanged"))
        yandexConfig(item)?.let { config ->
            config.copy(
                id = config.id,
                connectParams = config.connectParams.run {
                    key?.let {
                        this.copy(key = it)
                    } ?: this
                },
                methods = config.methods.run {
                    val m = this[Lookup]?.toMutableMap() ?: mutableMapOf()
                    lang?.let { lang ->
                        m[LookupLang] = lang
                    }
                    settings?.let { settings ->
                        m[LookupFlags] = settings.toInt().toString()
                    }

                    this + (Lookup to m)
                }
            )
        }?.let {
            configRepository.updateConfig(it)
        }
    }

    override fun onConfigAddClicked(type: ConfigCreateViewItem.Type) {
        analytics.send(AnalyticEvent.createActionEvent("DictConfigs.configAddClicked",
            mapOf("type" to type.name))
        )
        when (type) {
            ConfigCreateViewItem.Type.Online -> onYandexConfigAddClicked()
            ConfigCreateViewItem.Type.Offline -> onDslDictAddClicked()
        }
    }

    private fun onYandexConfigAddClicked() {
        analytics.send(AnalyticEvent.createActionEvent("DictConfigs.yandexConfigAddClicked"))
        val yandexConfig = Config(
            id = configRepository.value.data()?.maxOfOrNull { it.id + 1 } ?: 1,
            type = Config.Type.Yandex,
            connectParams = ConfigConnectParams(
                baseUrl = "https://dictionary.yandex.net/",
                key = "",
                securedKey = "",
            ),
            methods = mapOf(Lookup to mapOf(LookupLang to LookupLangDefault)),
        )
        configRepository.addConfig(yandexConfig)
    }

    private fun onDslDictAddClicked() {
        analytics.send(AnalyticEvent.createActionEvent("DictConfigs.dslDictAddClicked"))
        mainScope.launch(Dispatchers.Default) {
            dslDictOpenController.chooseFile()
                .onError {
                    router?.onError(it.toStringResource())
                }
        }
    }

    override fun onDictDeleted(item: ConfigDictViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("DictConfigs.dictDeleted"))
        mainScope.launch(Dispatchers.IO) {
            dictRepository.delete(item.path)
        }
    }

    override fun onRemoteDictStartLoadingClicked(item: RemoteDictViewItem) {
        val path = remoteDictRepository.loadedDictPath(item.dict)
        if (dslDictImportUseCase.isPathInProgress(path)) {
            return
        }

        analytics.send(AnalyticEvent.createActionEvent("DictConfigs.onRemoteDictLoadingStart", mapOf("name" to path.name)))
        mainScope.launch(Dispatchers.IO) {
            remoteDictRepository.loadDict(item.dict).collect {
                if (it.progress() == 1.0f) {
                    analytics.send(
                        AnalyticEvent.createActionEvent(
                            "DictConfigs.onRemoteDictLoadingComplete",
                            mapOf("name" to path.name)
                        )
                    )
                    try {
                        dslDictImportUseCase.run(path)
                        remoteDictRepository.clear(item.dict)
                        analytics.send(
                            AnalyticEvent.createActionEvent(
                                "DictConfigs.onRemoteDictImportComplete",
                                mapOf("name" to path.name)
                            )
                        )
                    } catch (e: Throwable) {
                        analytics.send(
                            AnalyticEvent.createErrorEvent(
                                "DictConfigsVM.onRemoteDictImportError",
                                e
                            )
                        )
                    }
                } else if (
                        it.onError {
                            analytics.send(
                                AnalyticEvent.createErrorEvent(
                                    "DictConfigsVM.onRemoteDictLoadingError",
                                    it
                                )
                            )
                        }
                    ) {
                }
            }
        }
    }

    override fun onCleared() {
    }
}

private fun Long.formatBytes(): String {
    if (this < 0) return "0 B"
    if (this < 1024) return "$this B"

    val units = arrayOf("KB", "MB", "GB", "TB", "PB", "EB")
    var value = this.toDouble()
    var unitIndex = -1

    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }

    return "%.1f %s".format(value, units[unitIndex])
}