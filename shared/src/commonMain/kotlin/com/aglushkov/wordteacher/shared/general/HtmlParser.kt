package com.aglushkov.wordteacher.shared.general

import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf

@Serializable(with = HtmlStringSerializer::class)
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
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.text)
            encodeSerializableElement(descriptor, 1, ListSerializer(HtmlLinkData.serializer()), value.links)
        }
    }

    override fun deserialize(decoder: Decoder): HtmlString {
        val string = decoder.decodeString()

        return try {
            // try parse as json
            decoder.decodeStructure(descriptor) {

            }
        } catch (_: Exception) {
            val parser = HtmlParser()
            parser.parse(string)
        }
    }
}
//
//object HtmlStringSerializer2 : KSerializer<HtmlString> {
//    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("com.aglushkov.HtmlString") {
//        element<String>("text")
//        element<List<String>>("lecturers")
//    }
//
//    override fun serialize(encoder: Encoder, value: HtmlString) {
//        encoder.encodeStructure(descriptor) {
//            encodeStringElement(descriptor, 0, value.uid)
//            encodeSerializableElement(descriptor, 1, InstantSerializer, value.start)
//            encodeSerializableElement(descriptor, 2, InstantSerializer, value.end)
//            encodeStringElement(descriptor, 3, value.module)
//            encodeSerializableElement(descriptor, 4, ListSerializer(String.serializer()), value.lecturers)
//            encodeNullableSerializableElement(descriptor, 5, String.serializer(), value.room)
//            encodeNullableSerializableElement(descriptor, 6, String.serializer(), value.type)
//            encodeNullableSerializableElement(descriptor, 7, String.serializer(), value.note)
//        }
//    }
//
//    override fun deserialize(decoder: Decoder): HtmlString {
//        return decoder.decodeStructure(descriptor) {
//            var uid: String? = null
//            var start: Instant? = null
//            var end: Instant? = null
//            var module: String? = null
//            var lecturers: List<String> = emptyList()
//            var room: String? = null
//            var type: String? = null
//            var note: String? = null
//
//            loop@ while (true) {
//                when (val index = decodeElementIndex(descriptor)) {
//                    DECODE_DONE -> break@loop
//
//                    0 -> uid = decodeStringElement(descriptor, 0)
//                    1 -> start = decodeSerializableElement(descriptor, 1, InstantSerializer)
//                    2 -> end = decodeSerializableElement(descriptor, 2, InstantSerializer)
//                    3 -> module = decodeStringElement(descriptor, 3)
//                    4 -> lecturers = decodeSerializableElement(descriptor, 4, ListSerializer(String.serializer()))
//                    5 -> room = decodeNullableSerializableElement(descriptor, 5, String.serializer().nullable)
//                    6 -> type = decodeNullableSerializableElement(descriptor, 6, String.serializer().nullable)
//                    7 -> note = decodeNullableSerializableElement(descriptor, 7, String.serializer().nullable)
//
//                    else -> throw SerializationException("Unexpected index $index")
//                }
//            }
//
//            Lesson(
//                requireNotNull(uid),
//                requireNotNull(start),
//                requireNotNull(end),
//                requireNotNull(module),
//                lecturers,
//                room,
//                type,
//                note
//            )
//        }
//    }
//}