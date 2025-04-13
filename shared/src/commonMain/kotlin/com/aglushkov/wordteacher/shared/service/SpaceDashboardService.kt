package com.aglushkov.wordteacher.shared.service

import co.touchlab.kermit.Logger
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.LinkDividerCharCategories
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.indexOfChar
import com.aglushkov.wordteacher.shared.general.HtmlString
import com.aglushkov.wordteacher.shared.general.Response
import com.aglushkov.wordteacher.shared.general.setStatusCode
import com.aglushkov.wordteacher.shared.model.CardSet
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.util.Stack

@Serializable
data class SpaceDashboardResponse(
    @SerialName("headlineBlock") val headlineBlock: SpaceDashboardHeadlineBlock,
    @SerialName("newCardSetBlock") val newCardSetBlock: SpaceDashboardNewCardSetBlock,
)

@Serializable
data class SpaceDashboardNewCardSetBlock(
    @SerialName("cardSets") val cardSets: List<CardSet> = emptyList(),
)

@Serializable
data class SpaceDashboardHeadlineBlock(
    @SerialName("categories") val categories: List<SpaceDashboardCategory> = emptyList(),
)

@Serializable
data class SpaceDashboardCategory(
    @SerialName("categoryName") val categoryName: String,
    @SerialName("headlines") val headlines: List<SpaceDashboardHeadline> = emptyList(),
)

@Serializable
data class SpaceDashboardHeadline(
    @SerialName("id") val id: String,
    @SerialName("sourceName") val sourceName: String,
    @SerialName("sourceCategory") val sourceCategory: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: HtmlString?,
    @SerialName("link") val link: String,
    @SerialName("date") val date: Instant,
    @SerialName("creator") val creator: String?,
)

class SpaceDashboardService(
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
                    subclass(Response.Ok.serializer(SpaceDashboardResponse.serializer()))
                    subclass(Response.Err.serializer())
                }
            }
        }
    }

    suspend fun load(): Response<SpaceDashboardResponse> {
        return withContext(Dispatchers.Default) {
            val res: HttpResponse = httpClient.get(urlString = "${baseUrl}/api/v1/dashboard")
            val stringResponse: String = res.body()
            json.decodeFromString<Response<SpaceDashboardResponse>>(stringResponse).setStatusCode(res.status.value)
        }
    }
}
