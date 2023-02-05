package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.general.ErrResponse
import com.aglushkov.wordteacher.shared.general.OkResponse
import com.aglushkov.wordteacher.shared.general.Response
import com.aglushkov.wordteacher.shared.model.CardSet
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data class CardSetPullInput(
    @SerialName("currentCardSetIds") val currentCardSetIds: List<String>,
)

@Serializable
data class CardSetPullResponse(
    @SerialName("updatedCardSets") val updatedCardSets: List<CardSet>,
    @SerialName("deletedCardSetIds") val deletedCardSetIds: List<String>,
)

@Serializable
data class CardSetPushInput(
    @SerialName("updatedCardSets") val updatedCardSets: List<CardSet>,
    @SerialName("currentCardSetIds") val currentCardSetIds: List<String>,
)

@Serializable
data class CardSetPushResponse(
    @SerialName("cardSetIds") val cardSetIds: Map<String,String>,
    @SerialName("cardIds") val cardIds: Map<String,String>,
)

class SpaceCardSetService(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {
    private val pullJson by lazy {
        Json {
            ignoreUnknownKeys = true
            classDiscriminator = "status"
            serializersModule = SerializersModule {
                polymorphic(Response::class) {
                    subclass(OkResponse.serializer(CardSetPullResponse.serializer()))
                    subclass(ErrResponse.serializer())
                }
            }
        }
    }

    private val pushJson by lazy {
        Json {
            ignoreUnknownKeys = true
            classDiscriminator = "status"
            serializersModule = SerializersModule {
                polymorphic(Response::class) {
                    subclass(OkResponse.serializer(CardSetPullResponse.serializer()))
                    subclass(ErrResponse.serializer())
                }
            }
        }
    }

    suspend fun pull(currentCardSetIds: List<String>, lastModificationDate: Instant?): Response<CardSetPullResponse> {
        val res: HttpResponse =
            httpClient.post(urlString = "${baseUrl}/api/cardsets/pull") {
                lastModificationDate?.let { date ->
                    this.parameter(LastModificationDateKey, date.toString())
                }
                this.setBody(pullJson.encodeToString(CardSetPullInput(currentCardSetIds)))
            }
        return withContext(Dispatchers.Default) {
            val stringResponse = res.readBytes().decodeToString()
            pullJson.decodeFromString(stringResponse)
        }
    }

    suspend fun push(updatedCardSets: List<CardSet>, currentCardSetIds: List<String>, lastModificationDate: Instant?): Response<CardSetPushResponse> {
        val res: HttpResponse =
            httpClient.post(urlString = "${baseUrl}/api/cardsets/push") {
                lastModificationDate?.let { date ->
                    this.parameter(LastModificationDateKey, date.toString())
                }
                this.setBody(pullJson.encodeToString(CardSetPushInput(updatedCardSets, currentCardSetIds)))
            }
        return withContext(Dispatchers.Default) {
            val stringResponse = res.readBytes().decodeToString()
            pullJson.decodeFromString(stringResponse)
        }
    }
}

private const val LastModificationDateKey = "latestCardSetModificationDate"
