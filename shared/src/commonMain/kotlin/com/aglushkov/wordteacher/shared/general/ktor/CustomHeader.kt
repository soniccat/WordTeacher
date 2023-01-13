package com.aglushkov.wordteacher.shared.general.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.util.AttributeKey

class CustomHeader(val config: Config) {

    class Config(var headerName: String = "", var headerValue: String = "")

    companion object Plugin : HttpClientPlugin<Config, CustomHeader> {
        override val key: AttributeKey<CustomHeader> = AttributeKey("CustomHeader")

        override fun prepare(block: Config.() -> Unit) = CustomHeader(Config().apply(block))

        override fun install(plugin: CustomHeader, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                context.header(plugin.config.headerName, plugin.config.headerValue)
            }
        }
    }
}
