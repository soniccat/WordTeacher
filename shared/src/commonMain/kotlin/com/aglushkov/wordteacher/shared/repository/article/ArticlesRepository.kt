package com.aglushkov.wordteacher.shared.repository.article

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.StringReader
import com.aglushkov.wordteacher.shared.general.StringsReader
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.resource.RESOURCE_UNDEFINED_PROGRESS
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.loadResourceWithProgress
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Article
import com.aglushkov.wordteacher.shared.model.ArticleStyle
import com.aglushkov.wordteacher.shared.model.Header
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
    private val timeSource: TimeSource,
    private val isDebug: Boolean,
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

    suspend fun createArticle(title: String, text: String): Flow<Resource<Article>> {
        return flow {
            Logger.v("start", "tag_createArticle")
            emit(Resource.Loading(progress = RESOURCE_UNDEFINED_PROGRESS))
            Logger.v("wait", "tag_createArticle")
            nlpCore.waitUntilInitialized()
            Logger.v("createArticleInternal", "tag_createArticle")
            createArticleInternal(title, text).collect {
                emit(it)
            }
            Logger.v("done", "tag_createArticle")
        }
    }

    suspend fun removeArticle(articleId: Long) = supervisorScope {
        scope.async(Dispatchers.Default) {
            removeArticleInternal(articleId)
        }.await()
    }

    private fun createArticleInternal(
        title: String,
        text: String
    ): Flow<Resource<Article>> {
       return loadResourceWithProgress(
           loader = processTextIntoArticle(text, title)
       )
    }

    class TagInfo(
        val isClose: Boolean = false,
        val name: String,
        var start: Int = 0,
        var end: Int = 0,
    )

    sealed interface ArticleTag {
        val isClose: Boolean
        var start: Int
        var end: Int

        data class Header(
            val size: Int,
            override val isClose: Boolean,
            override var start: Int,
            override var end: Int,
        ): ArticleTag

        companion object {
            fun parse(tagInfo: TagInfo): ArticleTag? =
                when (tagInfo.name[0]) {
                    'h' -> {
                        tagInfo.name.substring(1).toIntOrNull()?.let {
                            Header(size = it, tagInfo.isClose, tagInfo.start, tagInfo.end)
                        }
                    }
                    else -> null
                }

        }
    }

    fun StringsReader.readTag(): TagInfo? {
        if (!readUntil('<')) return null
        val startTagI = pos-1
        val isClose = peek(1) == "/"
        if (isClose) {
            seek(1)
        }
        val startTagNameI = pos

        if (!readUntil('>')) return null
        val endTagNameI = pos-1
        val endTagI = pos

        return TagInfo(
            isClose = isClose,
            name = substring(startTagNameI, endTagNameI),
            start = startTagI,
            end = endTagI,
        )
    }

    private fun cutStylesFromText(
        text: String
    ): Pair<String, ArticleStyle> {
        val allTags = mutableListOf<TagInfo>()
        val tagStack = ArrayDeque<TagInfo>()
        val tagPairs = mutableListOf<Pair<TagInfo, TagInfo>>()
        StringsReader(text).apply {
            while (!atEnd()) {
                val newTag = readTag() ?: break
                allTags.add(newTag)
                if (newTag.isClose) {
                    val topTag = tagStack.lastOrNull() ?: continue
                    if (topTag.name == newTag.name) {
                        tagPairs.add(Pair(topTag, newTag))
                        tagStack.removeLastOrNull()
                    } else {
                        Logger.v("invalid tag pair open:${topTag.name}, end:${newTag.name}", "cutStylesFromText")
                    }
                } else {
                    tagStack.addLast(newTag)
                }
            }
        }

        // build text without tags, updating tag indexes
        val newText = StringBuilder()
        var deletedCharCount = 0
        var newTextI = 0
        for (i in 0 until allTags.size) {
            val tag = allTags[i]
            ArticleTag.parse(tag) ?: continue

            newText.append(text.substring(newTextI, tag.start))
            newTextI = tag.end

            val tagLen = tag.end - tag.start
            tag.start -= deletedCharCount
            tag.end = tag.start
            deletedCharCount += tagLen

            if (i == allTags.size - 1) {
                newText.append(text.substring(newTextI))
            }
        }

        // build styles
        val headers = mutableListOf<Header>()
        tagPairs.onEach {
            val openTag = ArticleTag.parse(it.first)
            val closeTag = ArticleTag.parse(it.second)
            if (openTag != null && closeTag != null) {
                if (openTag is ArticleTag.Header) {
                    headers.add(Header(openTag.size, -1, openTag.start, closeTag.end))
                }
            }
        }

        return newText.toString() to ArticleStyle(headers = headers)
    }

    private fun processTextIntoArticle(
        text: String,
        title: String
    ): Flow<Pair<Float, Article?>> {
        var resultText = clearText(text)
        val cutResult = cutStylesFromText(resultText)
        resultText = cutResult.first

        val nlpCoreCopy = nlpCore.clone()
        val sentenceSpans = nlpCoreCopy.sentenceSpans(resultText)

        // update tag positions to be sentence relative
        var lastHeaderIndex = 0
        sentenceSpans.onEachIndexed { sI, sSpan ->
            for (hI in lastHeaderIndex until cutResult.second.headers.size) {
                val header = cutResult.second.headers[hI]
                if (header.start >= sSpan.start && header.end <= sSpan.end) {
                    header.sentenceIndex = sI
                    header.start -= sSpan.start
                    header.end -= sSpan.start
                    ++lastHeaderIndex
                } else {
                    break
                }
            }
        }

        val paragraphs = mutableListOf<Paragraph>()
        var startParagraphIndex = 0

        sentenceSpans.onEachIndexed { i, s ->
            if (i + 1 < sentenceSpans.size) {
                val nextS = sentenceSpans[i + 1]
                val sentenceGap = resultText.subSequence(s.end, nextS.start)
                if (sentenceGap.contains('\n')) {
                    paragraphs += Paragraph(startParagraphIndex, i + 1)
                    startParagraphIndex = i + 1

                    if (isDebug) {
                        val ds = resultText.subSequence(
                            sentenceSpans[paragraphs.last().start].start,
                            sentenceSpans[paragraphs.last().end - 1].end
                        ).toString()
                        Logger.v(ds)
                    }
                }
            }
        }

        if (startParagraphIndex < sentenceSpans.size) {
            paragraphs += Paragraph(startParagraphIndex, sentenceSpans.size)
        }

        val style = ArticleStyle(
            paragraphs = paragraphs,
            headers = cutResult.second.headers
        )
        val progressChannel = Channel<Pair<Float, Article?>>(UNLIMITED)
        scope.launch(Dispatchers.Default) {
            val article = database.transactionWithResult {
                val articleId = database.articles.run {
                    insert(title, timeSource.timeInMilliseconds(), style)
                    insertedArticleId()
                } ?: 0L

                val sentences = mutableListOf<NLPSentence>()
                val splitSentences = resultText.split(sentenceSpans)

                // TODO: process in parallel (split splitSentences by chunks and process them with await)
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
