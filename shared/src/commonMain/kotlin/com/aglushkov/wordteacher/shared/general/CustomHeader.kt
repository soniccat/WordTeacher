package com.aglushkov.wordteacher.shared.general

import io.ktor.client.HttpClient
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.util.AttributeKey

class CustomHeader(val config: Config) {

    class Config(var headerName: String = "", var headerValue: String = "")

    companion object Feature : HttpClientFeature<Config, CustomHeader> {
        override val key: AttributeKey<CustomHeader> = AttributeKey("CustomHeader")

        override fun prepare(block: Config.() -> Unit) = CustomHeader(Config().apply(block))

        override fun install(feature: CustomHeader, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                context.header(feature.config.headerName, feature.config.headerValue)
            }
        }
    }
}
