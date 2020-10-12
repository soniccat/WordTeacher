package com.aglushkov.wordteacher.repository

import com.aglushkov.wordteacher.apiproviders.google.service.GoogleService
import com.aglushkov.wordteacher.apiproviders.google.service.createWordTeacherWordService
import com.aglushkov.wordteacher.apiproviders.wordnik.service.WordnikService
import com.aglushkov.wordteacher.apiproviders.wordnik.service.createWordTeacherWordService
import com.aglushkov.wordteacher.apiproviders.yandex.service.YandexService
import com.aglushkov.wordteacher.apiproviders.yandex.service.createWordTeacherWordService
import com.aglushkov.wordteacher.shared.wordteacher.apiproviders.owlbot.service.OwlBotService
import com.aglushkov.wordteacher.shared.wordteacher.apiproviders.owlbot.service.createWordTeacherWordService
import com.aglushkov.wordteacher.shared.wordteacher.repository.Config
import com.aglushkov.wordteacher.shared.wordteacher.repository.ConfigConnectParams
import com.aglushkov.wordteacher.shared.wordteacher.repository.ServiceMethodParams
import com.aglushkov.wordteacher.shared.wordteacher.service.WordTeacherWordService

class WordTeacherWordServiceFactory {

    fun createService(
        type: Config.Type,
        connectParams: ConfigConnectParams,
        methodParams: ServiceMethodParams
    ): WordTeacherWordService? {

        val baseUrl = connectParams.baseUrl
        val key = connectParams.key

        return when (type) {
            Config.Type.OwlBot -> {
                OwlBotService.createWordTeacherWordService(baseUrl, key)
            }
            Config.Type.Yandex -> {
                YandexService.createWordTeacherWordService(baseUrl, key, methodParams)
            }
            Config.Type.Wordnik -> {
                WordnikService.createWordTeacherWordService(baseUrl, key, methodParams)
            }
            Config.Type.Google -> {
                GoogleService.createWordTeacherWordService(baseUrl, methodParams)
            }
            else -> null
        }
    }
}