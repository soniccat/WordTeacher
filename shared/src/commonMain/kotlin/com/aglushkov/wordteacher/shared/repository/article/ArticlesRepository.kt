package com.aglushkov.wordteacher.shared.repository.article

import com.aglushkov.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.ArticleStyle
import com.aglushkov.wordteacher.shared.model.Paragraph
import com.aglushkov.wordteacher.shared.model.ShortArticle
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.model.nlp.split
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class ArticlesRepository(
    private val database: AppDatabase,
    private val nlpCore: NLPCore,
    private val nlpSentenceProcessor: NLPSentenceProcessor,
    private val timeSource: TimeSource
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<List<ShortArticle>>>(Resource.Loading())

    val shortArticles: StateFlow<Resource<List<ShortArticle>>> = stateFlow

    init {
        scope.launch(Dispatchers.Default) {
            database.articles.selectAllShortArticles().asFlow().collect {
                val result = it.executeAsList()
                Logger.v("ArticleRepository loaded ${result.size} articles")
                stateFlow.value = Resource.Loaded(result)
            }
        }
    }

    suspend fun createArticle(title: String, text: String): Article = supervisorScope {
        // Async in the scope to avoid retaining the parent coroutine and to cancel immediately
        // when it cancels (when corresponding ViewModel is cleared for example)
        scope.async(Dispatchers.Default) {
            return@async createArticleInternal(title, text)
        }.await()
    }

    suspend fun removeArticle(articleId: Long) = supervisorScope {
        scope.async(Dispatchers.Default) {
            removeArticleInternal(articleId)
        }.await()
    }

    private suspend fun createArticleInternal(title: String, text: String): Article {
        nlpCore.waitUntilInitialized()

        val resultText = clearText(text)//clearString(text)
        val nlpCoreCopy = nlpCore.clone()
        val sentenceSpans = nlpCoreCopy.sentenceSpans(resultText)
        val paragraphs = mutableListOf<Paragraph>()
        var startParagraphIndex = 0

        sentenceSpans.onEachIndexed { i, s ->
            if (i + 1 < sentenceSpans.size) {
                val nextS = sentenceSpans[i + 1]
                val sentenceGap = text.subSequence(s.end, nextS.start)
                if (sentenceGap.contains('\n')) {
                    paragraphs += Paragraph(startParagraphIndex, i + 1)
                    startParagraphIndex = i + 1

                    val ds = text.subSequence(sentenceSpans[paragraphs.last().start].start, sentenceSpans[paragraphs.last().end-1].end).toString()
                    Logger.v(ds)
                }
            }
        }

        if (startParagraphIndex < sentenceSpans.size) {
            paragraphs += Paragraph(startParagraphIndex, sentenceSpans.size)
        }

        val style = ArticleStyle(
            paragraphs = paragraphs
        )

        var article: Article? = null
        database.transaction {
            val articleId = database.articles.run {
                insert(title, timeSource.timeInMilliseconds(), style)
                insertedArticleId()
            } ?: 0L

            val sentences = mutableListOf<NLPSentence>()
            resultText.split(sentenceSpans).forEachIndexed { index, s ->
                val nlpSentence = NLPSentence(articleId, index.toLong(), clearString(s.toString()))
                nlpSentenceProcessor.process(nlpSentence, nlpCoreCopy)
                database.sentencesNLP.insert(nlpSentence)
                sentences += nlpSentence
            }

            article = Article(
                articleId,
                title,
                timeSource.timeInMilliseconds(),
                sentences = sentences,
                style = style
            )
        }

        return article ?: throw ArticleInsertException("Article is null")
    }

    private fun removeArticleInternal(articleId: Long) {
        database.articles.run {
            removeArticle(articleId)
        }
    }

    // TODO: use string builder
    private fun clearString(s: String) = s.replace(13.toChar(), ' ')
        .replace(10.toChar(), ' ')
        .replace(9.toChar(), ' ')
        .replace(11.toChar(), ' ')
        .replace(13.toChar(), ' ')
        .replace(3.toChar(), ' ')
        .trim()

    private fun clearText(s: String) = s.replace("``", "\"")
        .replace("''", "\"")
        .replace(147.toChar(), '"')
        .replace(148.toChar(), '"')
        .replace(8220.toChar(), '"')
        .replace(8221.toChar(), '"')
}

class ArticleInsertException(message: String): RuntimeException(message)
