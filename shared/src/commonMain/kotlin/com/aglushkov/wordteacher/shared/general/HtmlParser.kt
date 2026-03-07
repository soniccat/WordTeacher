package com.aglushkov.wordteacher.shared.general

import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf

@Serializable
data class HtmlString(
    val text: String,
    val links: List<HtmlLinkData>,
)

@Serializable
data class HtmlLinkData(
    val start: Int,
    val end: Int,
    val href: String,
)

class HtmlParser {
    fun parse(html: String): HtmlString {
        val resultText = StringBuilder()
        val linkStack = ArrayDeque<HtmlLinkData>()
        val links = mutableListOf<HtmlLinkData>()

        val ksoupHtmlParser = KsoupHtmlParser(
            handler = object : KsoupHtmlHandler {
                override fun onText(text: String) {
                    resultText.append(text)
                }

                override fun onOpenTag(
                    name: String,
                    attributes: Map<String, String>,
                    isImplied: Boolean
                ) {
                    if (name == "a") {
                        attributes["href"]?.let { href ->
                            if (resultText.lastOrNull()?.isWhitespace() == false) {
                                resultText.append(' ')
                            }
                            linkStack.add(HtmlLinkData(resultText.length, -1, href))
                        }
                    }
                }

                override fun onCloseTag(name: String, isImplied: Boolean) {
                    if (name == "a") {
                        val linkData = linkStack.removeLast()
                        links.add(linkData.copy(end = resultText.length))
                    }
                }
            },
        )
        ksoupHtmlParser.write(html)
        ksoupHtmlParser.end()

        return HtmlString(
            text = resultText.toString(),
            links = links,
        )
    }
}

object HtmlStringSerializer : KSerializer<HtmlString> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("com.aglushkov.HtmlString", PrimitiveKind.STRING)
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val sr = json.serializersModule.serializer(typeOf<HtmlString>())

    override fun serialize(encoder: Encoder, value: HtmlString) {
        val string = json.encodeToString(sr, value)
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): HtmlString {
        val string = decoder.decodeString()

        return try {
            // try parse as json
            json.decodeFromString(sr as DeserializationStrategy<HtmlString>, string)
        } catch (_: Exception) {
            val parser = HtmlParser()
            parser.parse(string)
        }
    }
}
