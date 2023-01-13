package com.aglushkov.wordteacher.shared.general.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.parameter
import io.ktor.util.AttributeKey

class CustomParameter(val config: Config) {

    class Config(var parameterName: String = "", var parameterValue: String = "")

    companion object Plugin : HttpClientPlugin<Config, CustomParameter> {
        override val key: AttributeKey<CustomParameter> = AttributeKey("CustomParameter")

        override fun prepare(block: Config.() -> Unit) = CustomParameter(Config().apply(block))

        override fun install(plugin: CustomParameter, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                context.parameter(plugin.config.parameterName, plugin.config.parameterValue)
            }
        }
    }
}
