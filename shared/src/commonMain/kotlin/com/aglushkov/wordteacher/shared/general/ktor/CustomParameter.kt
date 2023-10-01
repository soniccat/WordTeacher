package com.aglushkov.wordteacher.shared.general.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.parameter
import io.ktor.util.AttributeKey

class CustomParameter(val config: Config) {

    class Config(
        var parameterName: String = "",
        var parameterValue: String? = null,
        var parameterProvider: (() -> String?)? = null
    )

    companion object Plugin : HttpClientPlugin<Config, CustomParameter> {
        override val key: AttributeKey<CustomParameter> = AttributeKey("CustomParameter")

        override fun prepare(block: Config.() -> Unit) = CustomParameter(Config().apply(block))

        override fun install(plugin: CustomParameter, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                val value = plugin.config.parameterValue ?: plugin.config.parameterProvider?.invoke()
                value?.let { v ->
                    context.parameter(plugin.config.parameterName, v)
                }
            }
        }
    }
}
