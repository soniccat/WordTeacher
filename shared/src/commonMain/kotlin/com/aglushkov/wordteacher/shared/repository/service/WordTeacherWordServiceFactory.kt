package com.aglushkov.wordteacher.shared.repository.service

import com.aglushkov.wordteacher.apiproviders.yandex.service.YandexService
import com.aglushkov.wordteacher.apiproviders.yandex.service.createWordTeacherWordService
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.createWordTeacherWordService
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.config.ConfigConnectParams
import com.aglushkov.wordteacher.shared.repository.config.ServiceMethodParams
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService

class WordTeacherWordServiceFactory {

    fun createService(
        type: Config.Type,
        connectParams: ConfigConnectParams,
        methodParams: ServiceMethodParams
    ): WordTeacherWordService? {

        val baseUrl = connectParams.baseUrl
        val key = connectParams.key

        return when (type) {
            Config.Type.Yandex -> YandexService.createWordTeacherWordService(baseUrl, key, methodParams)
            Config.Type.WordTeacher -> WordTeacherDictService.createWordTeacherWordService(baseUrl)
            else -> null
        }
    }
}
