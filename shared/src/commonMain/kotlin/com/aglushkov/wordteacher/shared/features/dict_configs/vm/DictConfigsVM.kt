package com.aglushkov.wordteacher.shared.features.dict_configs.vm

import com.aglushkov.wordteacher.apiproviders.yandex.service.YandexService
import com.aglushkov.wordteacher.apiproviders.yandex.service.YandexService.Companion.Lookup
import com.aglushkov.wordteacher.apiproviders.yandex.service.YandexService.Companion.LookupFlags
import com.aglushkov.wordteacher.apiproviders.yandex.service.YandexService.Companion.LookupLang
import com.aglushkov.wordteacher.apiproviders.yandex.service.YandexService.Companion.LookupLangDefault
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.item.generateViewItemIds
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.config.ConfigConnectParams
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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

    fun onConfigDeleteClicked(item: YandexConfigViewItem)
    fun onYandexConfigChanged(item: YandexConfigViewItem, key: String?, lang: String?, settings: YandexSettings?)
    fun onYandexConfigAddClicked()
}

open class DictConfigsVMImpl(
    private val configRepository: ConfigRepository,
    private val idGenerator: IdGenerator,
): ViewModel(), DictConfigsVM {

    override val viewItems = configRepository.flow.map { res ->
        res.data().orEmpty().filter {
            it.type == Config.Type.Yandex
        }.map { config ->
            val flag = config.methods[Lookup]?.get(LookupFlags)?.toIntOrNull() ?: 0
            YandexConfigViewItem(
                id = config.id.toLong(),
                hasToken = config.connectParams.securedKey.isNotEmpty(),
                lang = config.methods[Lookup]?.get(LookupLang) ?: LookupLangDefault,
                settings = YandexSettings.fromInt(flag)
            ) as BaseViewItem<*>
        } + CreateConfigViewItem(Long.MAX_VALUE)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    override fun onConfigDeleteClicked(item: YandexConfigViewItem) {
        yandexConfig(item)?.let {
            configRepository.removeConfig(it.id)
        }
    }

    private fun yandexConfig(item: YandexConfigViewItem) =
        configRepository.value.data()
            ?.firstOrNull { it.id == item.id.toInt() }

    override fun onYandexConfigChanged(item: YandexConfigViewItem, key: String?, lang: String?, settings: YandexSettings?) {
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

    override fun onYandexConfigAddClicked() {
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

    override fun onCleared() {
    }
}
