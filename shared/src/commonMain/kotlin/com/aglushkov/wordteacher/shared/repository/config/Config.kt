package com.aglushkov.wordteacher.shared.repository.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// Translation service config
// Provides method description with paras for every service
@Serializable
data class Config(
    @SerialName("type") val type: Type,
    @SerialName("connectParams") val connectParams: List<ConfigConnectParams>,
    @SerialName("methods") val methods: Map<String, Map<String, String>>
    //@SerialName("methods") val methods: ServiceMethodParams = ServiceMethodParams(emptyMap())
) {

    @Serializable
    enum class Type {
        @SerialName("google") Google,
        @SerialName("owlbot") OwlBot,
        @SerialName("wordnik") Wordnik,
        @SerialName("yandex") Yandex,
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
