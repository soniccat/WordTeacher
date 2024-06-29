package com.aglushkov.wordteacher.shared.repository.article

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.loadResourceWithProgress
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

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

    suspend fun createArticle(title: String, text: String): Flow<Resource<Article>> /*supervisorScope*/ {
        return flow {
            createArticleInternal(title, text).collect {
                emit(it)
            }
        }
    }

    suspend fun removeArticle(articleId: Long) = supervisorScope {
        scope.async(Dispatchers.Default) {
            removeArticleInternal(articleId)
        }.await()
    }

    private suspend fun createArticleInternal(
        title: String,
        text: String
    ): Flow<Resource<Article>> {
        nlpCore.waitUntilInitialized()
       return loadResourceWithProgress(
           loader = processTextIntoArticle(text, title)
       )
    }

    private fun processTextIntoArticle(
        text: String,
        title: String
    ): Flow<Pair<Float, Article?>> {
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

                    val ds = text.subSequence(
                        sentenceSpans[paragraphs.last().start].start,
                        sentenceSpans[paragraphs.last().end - 1].end
                    ).toString()
                    Logger.v(ds)
                }
            }
        }

        if (startParagraphIndex < sentenceSpans.size) {
            paragraphs += Paragraph(startParagraphIndex, sentenceSpans.size)
        }

        val style = ArticleStyle(paragraphs = paragraphs)
        val progressChannel = Channel<Pair<Float, Article?>>(UNLIMITED)
        scope.launch(Dispatchers.Default) {
            val article = database.transactionWithResult {
                val articleId = database.articles.run {
                    insert(title, timeSource.timeInMilliseconds(), style)
                    insertedArticleId()
                } ?: 0L

                val sentences = mutableListOf<NLPSentence>()
                val splitSentences = resultText.split(sentenceSpans)

                // TODO: process in parallel (split splitSentences by chunks and process wthem with await)
                splitSentences.forEachIndexed { index, s ->
                    val nlpSentence = NLPSentence(articleId, index.toLong(), clearString(s.toString()))
                    nlpSentenceProcessor.process(nlpSentence, nlpCoreCopy)
                    database.sentencesNLP.insert(nlpSentence)
                    sentences += nlpSentence

                    progressChannel.trySend((index + 1).toFloat() / splitSentences.size to null)
                }

                Article(
                    articleId,
                    title,
                    timeSource.timeInMilliseconds(),
                    sentences = sentences,
                    style = style
                )
            }
            progressChannel.trySend(1.0f to article)
            progressChannel.close()
        }

        return progressChannel.receiveAsFlow()
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
