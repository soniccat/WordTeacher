package com.aglushkov.wordteacher.shared.repository.cardset

import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.features.cardset_json_import.vm.ImportCard
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.toPartOfSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

interface CardEnricher {

    suspend fun enrich(targets: List<Target>): List<Result>

    interface Target {
        val term: String
        val transcriptions: List<String>?
        val audioFiles: List<WordTeacherWord.AudioFile>?
        val partOfSpeech: WordTeacherWord.PartOfSpeech
        val examples: List<String>?
    }

    data class Result(
        var transcriptions: List<String>? = null,
        var audioFiles: List<WordTeacherWord.AudioFile>? = null,
        var partOfSpeech: WordTeacherWord.PartOfSpeech = WordTeacherWord.PartOfSpeech.Undefined,
    )
}

class CardEnricherImpl(
    private val wordTeacherDictService: WordTeacherDictService,
    private val nlpCore: NLPCore
): CardEnricher {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override suspend fun enrich(targets: List<CardEnricher.Target>): List<CardEnricher.Result> = withContext(Dispatchers.Default) {
        // pull info from dict service
        val loadedWords = wordTeacherDictService.loadWords(
            targets.map { it.term }
        ).toOkResponse().words.orEmpty()

        targets.map { card ->
            val result = CardEnricher.Result()
            val firstWord = loadedWords.firstOrNull { it.word == card.term }
            firstWord?.let { word ->
                if (card.transcriptions.isNullOrEmpty() && word.transcriptions.orEmpty().isNotEmpty()) {
                    result.transcriptions = word.transcriptions
                }

                if (card.audioFiles.isNullOrEmpty() && word.audioFiles.isNotEmpty()) {
                    result.audioFiles = word.audioFiles.map {
                        WordTeacherWord.AudioFile(
                            url = it.url,
                            accent = it.accent,
                            transcription = it.transcription,
                            text = it.text,
                        )
                    }
                }
            }

            nlpCore.waitUntilInitialized()
            if (card.partOfSpeech == WordTeacherWord.PartOfSpeech.Undefined) {
                // if there's only one type of speech in dictionary, just take it
                val avalilablePartOfSpeeches = loadedWords.filter {
                    it.word == card.term
                }.map {
                    it.defPairs
                }.flatten().map {
                    it.partOfSpeech
                }.distinct()

                result.partOfSpeech = if (avalilablePartOfSpeeches.size == 1) {
                    avalilablePartOfSpeeches.first()
                } else {
                    // otherwise try to resolve with nlp
                    tryResolvePartOfSpeech(card.term, card.examples.orEmpty())
                }
            }

            result
        }
    }


    private fun tryResolvePartOfSpeech(term: String, examples: List<String>): WordTeacherWord.PartOfSpeech {
        examples.onEach {
            val partOfSpeech = tryResolvePartOfSpeech(term, it)
            if (partOfSpeech != WordTeacherWord.PartOfSpeech.Undefined) {
                return partOfSpeech
            }
        }

        return WordTeacherWord.PartOfSpeech.Undefined
    }

    // inspired by findTermSpans
    private fun tryResolvePartOfSpeech(term: String, text: String): WordTeacherWord.PartOfSpeech {
        val nlpSentence = NLPSentence(text = nlpCore.normalizeText(text))
        nlpSentence.tokenSpans = nlpCore.tokenSpans(nlpSentence.text)
        nlpSentence.tags = nlpCore.tag(nlpSentence.tokenStrings())
        nlpSentence.lemmas = nlpCore.lemmatize(nlpSentence.tokenStrings(), nlpSentence.tags)

        val words = term.split(' ')
        var tokenI = 0
        var wordI = 0
        val partOfSpeechMap = mutableMapOf<WordTeacherWord.PartOfSpeech, Int>()
        while (tokenI < nlpSentence.tokenSpans.size && wordI < words.size) {
            val word = words[wordI].lowercase()
            val token = nlpSentence.token(tokenI).toString().lowercase()
            val lemma = nlpSentence.lemma(tokenI)?.lowercase()

            if (token == word || lemma == word || token.startsWith(word)) {
                val partOfSpeech = nlpSentence.tagEnum(tokenI).toPartOfSpeech()
                partOfSpeechMap[partOfSpeech] = partOfSpeechMap.getOrDefault(partOfSpeech, 0) + 1

                if (wordI == words.size - 1) {
                    wordI = 0
                } else {
                    ++wordI
                }
            }

            ++tokenI
        }

        return partOfSpeechMap.toList().maxByOrNull {
            it.second
        }?.first ?: WordTeacherWord.PartOfSpeech.Undefined
    }
}
