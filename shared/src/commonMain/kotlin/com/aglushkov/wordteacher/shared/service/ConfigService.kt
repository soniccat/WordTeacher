package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.repository.Config
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class ConfigService(
    private val baseUrl: String
) {
    companion object {}

    private val httpClient = HttpClient { }

    //@GET("wordteacher/config")
    suspend fun config(): List<Config> {
        val res: HttpResponse = httpClient.get("${baseUrl}wordteacher/config")
        return withContext(Dispatchers.Default) {
            Json {
                ignoreUnknownKeys = true
            }.decodeFromString(res.readBytes().decodeToString())
        }
    }
}

//fun ConfigService.Companion.create(aBaseUrl: String): ConfigService =
//        createRetrofit(aBaseUrl).create(ConfigService::class.java)
//
//fun ConfigService.Companion.decodeConfigs(body: ResponseBody): List<Config> {
//    val byteArray = body.bytes()
//    return decodeConfigs(byteArray)
//}
//
//fun ConfigService.Companion.decodeConfigs(byteArray: ByteArray): List<Config> {
//    // TODO: decipher the content
//    val gson = GsonBuilder().create()
//    val type = object : TypeToken<List<Config>>() {}.type
//    return gson.fromJson(byteArray.toString(StandardCharsets.UTF_8), type)
//}
//
//fun ConfigService.Companion.encodeConfigs(configs: List<Config>): ByteArray {
//    // TODO: cipher the content
//    val gson = GsonBuilder().create()
//    return gson.toJson(configs).toByteArray()
//}