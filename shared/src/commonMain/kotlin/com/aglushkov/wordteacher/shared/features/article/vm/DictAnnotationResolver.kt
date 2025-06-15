package com.aglushkov.wordteacher.shared.features.article.vm

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.PhraseSpan
import com.aglushkov.wordteacher.shared.model.nlp.spanWithIndex

class DictAnnotationResolver {

    fun resolve(
        actualDicts: List<Dict>,
        sentence: NLPSentence,
        phrases: List<PhraseSpan>,
    ): List<ArticleAnnotation.DictWord> {
        val dictAnnotations = actualDicts.map { dict ->
            val annotations = mutableListOf<ArticleAnnotation.DictWord>()
            var i = 0

            while (i < sentence.lemmas.size) {
                val firstWord = sentence.lemmaOrToken(i).toString().replace("\"", "") // TODO: try to train a better tokenizer model
                val isVerb = sentence.tagEnum(i).isVerb()
                var skippedNounPhrase = false

                var ci = i
                val wordFormsProvider: (Int) -> List<String> = { index ->
                    val lemma = sentence.lemma(index)
                    val token = sentence.token(index)
                    buildList {
                        var lowercasedLemma: String? = null
                        if (lemma != null) {
                            add(lemma)
                            lowercasedLemma = lemma.lowercase()
                            if (lowercasedLemma != lemma) {
                                add(lowercasedLemma)
                            }
                        }

                        val tokenStr = token.toString()
                        if (tokenStr != lemma && tokenStr != lowercasedLemma) {
                            add(tokenStr)
                        }
                        val lowercasedToken = tokenStr.lowercase()
                        if (lowercasedToken != tokenStr && lowercasedToken != lemma && lowercasedToken != lowercasedLemma) {
                            add(lowercasedToken)
                        }
                    }
                }

                var nextCallCount = 0
                val nextWordFormHandler = {
                    if (nextCallCount > 0) {
                        val phrase = phrases.spanWithIndex(ci)
                        if (phrase?.type?.isNounPhrase() == true && isVerb && firstWord != "be" && !skippedNounPhrase) {
                            if (ci == phrase.start && ci + phrase.length < sentence.lemmas.size) {
                                skippedNounPhrase = true
                                ci += phrase.length
                                wordFormsProvider(ci)
                            } else {
                                emptyList()
                            }
                        } else if (sentence.isAdverbNotPart(ci) || sentence.tagEnum(ci)
                                .isPronoun() || sentence.tagEnum(ci).isNoun()
                        ) {
                            if (ci + 1 < sentence.lemmas.size) {
                                ++ci
                                wordFormsProvider(ci)
                            } else {
                                emptyList()
                            }
                        } else {
                            emptyList()
                        }
                    } else {
                        ++nextCallCount
                        if (ci + 1 < sentence.lemmas.size) {
                            ++ci
                            wordFormsProvider(ci)
                        } else {
                            emptyList()
                        }
                    }
                }

                val foundList = mutableListOf<Pair<Int, List<Dict.Index.Entry>>>()
                dict.index.entry(
                    firstWord,
                    nextWordForms = nextWordFormHandler,
                    onWordRead = {
                        nextCallCount = 0
                    },
                    onFound = {
                        nextCallCount = 0
                        foundList.add(ci to it)
                    }
                )

                // we might went too deep in the dictionary
                // try to treat the next word as not a part of the verb phrase, let's assume it's a part of the next noun phrase
                // For example look at testPhraseHighlightWithNounPhraseInTheMiddle test
                if (foundList.isEmpty() && isVerb && ci > i + 1) {
                    ci = i + 1
                    nextCallCount = 1
                    dict.index.entry(
                        firstWord,
                        nextWordForms = nextWordFormHandler,
                        onWordRead = {
                            nextCallCount = 0
                        },
                        onFound = {
                            nextCallCount = 0
                            foundList.add(ci to it)
                        }
                    )
                }

                foundList.lastOrNull().takeIf { it?.second?.isNotEmpty() == true }?.let {
                    annotations.add(
                        ArticleAnnotation.DictWord(
                            start = sentence.tokenSpans[i].start,
                            end = sentence.tokenSpans[it.first].end,
                            entry = it.second.last(),
                            dict = dict
                        )
                    )
                }

                ++i
            }

            annotations
        }.flatten()
        return dictAnnotations
    }
}
