package com.aglushkov.wordteacher.shared.features.article.vm

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.PhraseSpan
import com.aglushkov.wordteacher.shared.model.nlp.spanWithIndex

class DictAnnotationResolver {

    fun resolve(
        actualDicts: List<Dict>,
        sentence: NLPSentence,
        phrases: List<PhraseSpan>
    ): List<ArticleAnnotation.DictWord> {
        val dictAnnotations = actualDicts.map { dict ->
            val annotations = mutableListOf<ArticleAnnotation.DictWord>()
            var i = 0

            while (i < sentence.lemmas.size) {
                val firstWord = sentence.lemmaOrToken(i).toString().lowercase()
                val isVerb = sentence.tagEnum(i).isVerb()
                var skippedNounPhrase = false

                var ci = i
                var takeLemma = true
                val tokenLemmaGetter: (Int) -> String? = { index ->
                    var r: CharSequence? = if (takeLemma) {
                        sentence.lemma(index)
                    } else {
                        null
                    }

                    if (r == null) {
                        takeLemma = false
                        r = sentence.token(index)
                    }
                    r.toString().lowercase()
                }

                val foundList = mutableListOf<Pair<Int, List<Dict.Index.Entry>>>()
                dict.index.entry(
                    firstWord,
                    nextWord = { needAnotherOne ->
                        if (needAnotherOne) {
                            val phrase = phrases.spanWithIndex(ci)
                            if (takeLemma) {
                                takeLemma = false
                                tokenLemmaGetter.invoke(ci)
                            } else if (phrase?.type?.isNounPhrase() == true && isVerb && firstWord != "be" && !skippedNounPhrase) {
                                if (ci + phrase.length < sentence.lemmas.size) {
                                    skippedNounPhrase = true
                                    ci += phrase.length
                                    tokenLemmaGetter.invoke(ci)
                                } else {
                                    null
                                }
                            } else if (sentence.isAdverbNotPart(ci) || sentence.tagEnum(ci)
                                    .isPronoun() || sentence.tagEnum(ci).isNoun()
                            ) {
                                if (ci + 1 < sentence.lemmas.size) {
                                    ++ci
                                    tokenLemmaGetter.invoke(ci)
                                } else {
                                    null
                                }
                            } else {
                                null
                            }
                        } else {
                            takeLemma = true
                            if (ci + 1 < sentence.lemmas.size) {
                                ++ci
                                tokenLemmaGetter.invoke(ci)
                            } else {
                                null
                            }
                        }
                    },
                    onFound = {
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
                    i = it.first
                }

                ++i
            }

            annotations
        }.flatten()
        return dictAnnotations
    }
}
