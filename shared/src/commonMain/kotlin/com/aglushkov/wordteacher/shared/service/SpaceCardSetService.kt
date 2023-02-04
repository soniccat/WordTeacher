package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.general.ErrResponse
import com.aglushkov.wordteacher.shared.general.OkResponse
import com.aglushkov.wordteacher.shared.general.Response
import io.ktor.client.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    @SerialName("updatedCardSets") val updatedCardSets: List<String>,
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
                    subclass(OkResponse.serializer(AuthData.serializer()))
                    subclass(ErrResponse.serializer())
                }
            }
        }
    }
}