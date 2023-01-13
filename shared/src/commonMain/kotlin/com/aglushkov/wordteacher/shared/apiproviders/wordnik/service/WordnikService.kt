package com.aglushkov.wordteacher.apiproviders.wordnik.service

import com.aglushkov.wordteacher.apiproviders.wordnik.model.WordnikWord
import com.aglushkov.wordteacher.apiproviders.wordnik.model.asWordTeacherWords
import com.aglushkov.wordteacher.shared.apiproviders.WordServiceLogger
import com.aglushkov.wordteacher.shared.general.ktor.CustomParameter
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.config.ServiceMethodParams
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class WordnikService(
    private val baseUrl: String,
    private val apiKey: String
) {
    enum class Dictionary(val value: String) {
        All("all"),
        Ahd5("ahd-5"),
        AhdLegacy("ahd-legacy"),
        Century("century"),
        Wiktionary("wiktionary"),
        Webster("webster"),
        Wordnet("wordnet");
    }

    enum class PartOfSpeech(value: String) {
        Noun("noun"),
        Adjective("adjective"),
        Verb("verb"),
        Adverb("adverb"),
        Interjection("interjection"),
        Pronoun("pronoun"),
        Preposition("preposition"),
        Abbreviation("abbreviation"),
        Affix("affix"),
        Article("article"),
        AuxiliaryVerb("auxiliary-verb "),
        Conjunction("conjunction"),
        DefiniteArticle("definite-article"),
        FamilyName("family-name"),
        GivenName("given-name"),
        Idiom("idiom"),
        Imperative("imperative"),
        NounPlural("noun-plural"),
        NounPossessive("noun-posessive"),
        PastParticiple("past-participle"),
        PhrasalPrefix("phrasal-prefix"),
        ProperNoun("proper-noun"),
        ProperNounPlural("proper-noun-plural"),
        ProperNounPossessive("proper-noun-posessive"),
        Suffix("suffix"),
        VerbIntransitive("verb-intransitive"),
        VerbTransitive("verb-transitive"),
    }

    companion object {
        val Definitions = "wordnik_definitions"
        val DefinitionsSourceDictionaries = "wordnik_definitions_sourceDictionaries"
        val DefinitionsLimit = "wordnik_definitions_limit"
        val DefinitionsPartOfSpeech = "wordnik_definitions_partOfSpeech"
        val DefinitionsIncludeRelated = "wordnik_definitions_includeRelated"
        val DefinitionsUseCanonical = "wordnik_definitions_useCanonical"
        val DefinitionsIncludeTags = "wordnik_definitions_includeTags"
    }

    private val logger = WordServiceLogger(Config.Type.Wordnik.name)
    private val httpClient = HttpClient {
        val anApiKey = apiKey
        install(CustomParameter) {
            parameterName = "api_key"
            parameterValue = anApiKey
        }
    }

    suspend fun loadDefinitions(
        word: String,
        dictionaries: String,
        limit: Int,
        partOfSpeech: String?,
        includeRelated: Boolean,
        useCanonical: Boolean,
        includeTags: Boolean
    ): List<WordnikWord> {
        logger.logLoadingStarted(word)

        val res: HttpResponse = httpClient.get("${baseUrl}v4/word.json/${word}/definitions") {
            parameter("sourceDictionaries", dictionaries)
            parameter("limit", limit)
            parameter("partOfSpeech", partOfSpeech)
            parameter("includeRelated", includeRelated)
            parameter("useCanonical", useCanonical)
            parameter("includeTags", includeTags)
        }
        return withContext(Dispatchers.Default) {
            val responseString = res.readBytes().decodeToString()
            logger.logLoadingCompleted(word, res, responseString)

            Json {
                ignoreUnknownKeys = true
            }.decodeFromString(responseString)
        }
    }
}

fun WordnikService.Companion.createWordTeacherWordService(
    aBaseUrl: String,
    aKey: String,
    params: ServiceMethodParams
): WordTeacherWordService {
    return object : WordTeacherWordService {
        override var type: Config.Type = Config.Type.Wordnik
        private val service = WordnikService(aBaseUrl, aKey)

        override suspend fun define(word: String): List<WordTeacherWord> {
            val definitions = params.value[Definitions]
            val dictionaries = definitions?.get(DefinitionsSourceDictionaries) ?: WordnikService.Dictionary.Wiktionary.value
            val limit = definitions?.get(DefinitionsLimit)?.toIntOrNull() ?: 20
            val partOfSpeech = definitions?.get(DefinitionsPartOfSpeech)
            val includeRelated = definitions?.get(DefinitionsIncludeRelated)?.toBoolean() ?: false
            val useCanonical = definitions?.get(DefinitionsUseCanonical)?.toBoolean() ?: false
            val includeTags = definitions?.get(DefinitionsIncludeTags)?.toBoolean() ?: false

            return service.loadDefinitions(word,
                    dictionaries,
                    limit,
                    partOfSpeech,
                    includeRelated,
                    useCanonical,
                    includeTags).asWordTeacherWords()
        }
    }
}