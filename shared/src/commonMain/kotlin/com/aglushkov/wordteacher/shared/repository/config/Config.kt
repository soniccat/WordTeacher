package com.aglushkov.wordteacher.shared.repository.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// Translation service config
// Provides method description with params for every service
@Serializable
data class Config(
    @SerialName("type") val type: Type,
    @SerialName("connectParams") val connectParams: ConfigConnectParams,
    @SerialName("methods") val methods: Map<String, Map<String, String>>
    //@SerialName("methods") val methods: ServiceMethodParams = ServiceMethodParams(emptyMap())
) {

    @Serializable
    enum class Type {
        @SerialName("yandex") Yandex,
        @SerialName("wordteacher") WordTeacher,
        @SerialName("local") Local
    }
}

@Serializable
data class ConfigConnectParams(
    @SerialName("baseUrl") val baseUrl: String,
    @SerialName("key") val key: String
)

@Serializable
class ServiceMethodParams(val value: Map<String, Map<String, String>>)
