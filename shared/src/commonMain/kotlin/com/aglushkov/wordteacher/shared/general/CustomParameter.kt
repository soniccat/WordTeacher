package com.aglushkov.wordteacher.shared.general

import io.ktor.client.HttpClient
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.util.AttributeKey

class CustomParameter(val config: Config) {

    class Config(var parameterName: String = "", var parameterValue: String = "")

    companion object Feature : HttpClientFeature<Config, CustomParameter> {
        override val key: AttributeKey<CustomParameter> = AttributeKey("CustomParameter")

        override fun prepare(block: Config.() -> Unit) = CustomParameter(Config().apply(block))

        override fun install(feature: CustomParameter, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                context.parameter(feature.config.parameterName, feature.config.parameterValue)
            }
        }
    }
}
