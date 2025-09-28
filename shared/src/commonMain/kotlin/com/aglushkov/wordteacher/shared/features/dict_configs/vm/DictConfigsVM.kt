package com.aglushkov.wordteacher.shared.features.dict_configs.vm

import com.aglushkov.wordteacher.shared.apiproviders.yandex.service.YandexService.Companion.Lookup
import com.aglushkov.wordteacher.shared.apiproviders.yandex.service.YandexService.Companion.LookupFlags
import com.aglushkov.wordteacher.shared.apiproviders.yandex.service.YandexService.Companion.LookupLang
import com.aglushkov.wordteacher.shared.apiproviders.yandex.service.YandexService.Companion.LookupLangDefault
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.wordlist.WordListDict
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.on
import com.aglushkov.wordteacher.shared.general.toStringDesc
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.config.ConfigConnectParams
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import dev.icerock.moko.resources.desc.ResourceStringDesc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.aglushkov.wordteacher.shared.res.MR

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

interface DictConfigsVM: Clearable {
    val viewItems: StateFlow<List<BaseViewItem<*>>>

    fun onConfigDeleteClicked(item: ConfigYandexViewItem)
    fun onYandexConfigChanged(item: ConfigYandexViewItem, key: String?, lang: String?, settings: YandexSettings?)
    fun onConfigAddClicked(type: ConfigCreateViewItem.Type)
    fun onDictDeleted(item: ConfigDictViewItem)
}

open class DictConfigsVMImpl(
    private val configRepository: ConfigRepository,
    private val dslDictOpenController: FileOpenController,
    private val dictRepository: DictRepository,
    private val idGenerator: IdGenerator,
    private val analytics: Analytics,
): ViewModel(), DictConfigsVM {

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override val viewItems = combine(
        configRepository.flow,
        dictRepository.dicts
    ) { configs, dicts ->
        buildViewItems(configs, dicts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun onConfigDeleteClicked(item: ConfigYandexViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("DictConfigs.configDeleteClicked"))
        yandexConfig(item)?.let {
            configRepository.removeConfig(it.id)
        }
    }

    private fun yandexConfig(item: ConfigYandexViewItem) =
        configRepository.value.data()
            ?.firstOrNull { it.id == item.id.toInt() }

    private fun buildViewItems(configs: Resource<List<Config>>, dicts: Resource<List<Dict>>): List<BaseViewItem<*>> = buildList<BaseViewItem<*>> {
        add(ConfigHeaderViewItem(ResourceStringDesc(MR.strings.dictconfigs_online_section_title)))
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
                add(ConfigTextViewItem(it.toStringDesc()))
            }
        )

        add(ConfigCreateViewItem(ConfigCreateViewItem.Type.Online))
        add(ConfigHeaderViewItem(ResourceStringDesc(MR.strings.dictconfigs_offline_section_title)))
        dicts.on(
            loaded = {
                it.onEach { dict ->
                    if (dict !is WordListDict) {
                        add(ConfigDictViewItem(dict.name, dict.path))
                    }
                }
            },
            loading = {
                add(ConfigLoadingViewItem())
            },
            error = {
                add(ConfigTextViewItem(it.toStringDesc()))
            }
        )
        add(ConfigCreateViewItem(ConfigCreateViewItem.Type.Offline))
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
        }
    }

    override fun onDictDeleted(item: ConfigDictViewItem) {
        analytics.send(AnalyticEvent.createActionEvent("DictConfigs.dictDeleted"))
        dictRepository.delete(item.path)
    }

    override fun onCleared() {
    }
}
