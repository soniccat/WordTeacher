package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.general.Response
import com.aglushkov.wordteacher.shared.general.setStatusCode
import com.aglushkov.wordteacher.shared.model.CardSet
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data class CardSetSearchResponse(
    @SerialName("cardSets") val cardSets: List<CardSet>?,
)

class SpaceCardSetSearchService(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {
    private val json by lazy {
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
            classDiscriminator = "status"
            coerceInputValues = true
            serializersModule = SerializersModule {
                polymorphic(Response::class) {
                    subclass(Response.Ok.serializer(CardSetSearchResponse.serializer()))
                    subclass(Response.Err.serializer())
                }
            }
        }
    }

    suspend fun search(query: String): Response<CardSetSearchResponse> {
        val res: HttpResponse =
            httpClient.get(urlString = "${baseUrl}/api/cardsets/search") {
                url {
                    parameter("query", query)
                }
            }
        return withContext(Dispatchers.Default) {
            val stringResponse: String = res.body()
            json.decodeFromString<Response<CardSetSearchResponse>>(stringResponse).setStatusCode(res.status.value)
        }
    }
}
