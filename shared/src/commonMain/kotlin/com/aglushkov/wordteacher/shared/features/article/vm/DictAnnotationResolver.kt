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
                val firstWord = sentence.lemmaOrToken(i).toString()
                val isVerb = sentence.tagEnum(i).isVerb()
                var skippedNounPhrase = false

                var ci = i
                val wordFormsProvider: (Int) -> List<String> = { index ->
                    val lemma = sentence.lemma(index).toString()
                    val token = sentence.token(index).toString()
                    buildList {
                        add(lemma)
                        if (lemma != token) {
                            add(token)
                        }
                    }
                }

                var nextCallCount = 0
                val foundList = mutableListOf<Pair<Int, List<Dict.Index.Entry>>>()
                dict.index.entry(
                    firstWord,
                    nextWordForms = {
                        if (nextCallCount > 0) {
                            val phrase = phrases.spanWithIndex(ci)
                            if (phrase?.type?.isNounPhrase() == true && isVerb && firstWord != "be" && !skippedNounPhrase) {
                                if (ci + phrase.length < sentence.lemmas.size) {
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
                    },
                    onWordRead = {
                        nextCallCount = 0
                    },
                    onFound = {
                        nextCallCount = 0
                        foundList.add(ci to it)
                    }
                )

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
