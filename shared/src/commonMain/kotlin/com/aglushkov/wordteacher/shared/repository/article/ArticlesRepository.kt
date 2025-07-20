package com.aglushkov.wordteacher.shared.repository.article

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.StringsReader
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.extensions.updateLoadedData
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilLoaded
import com.aglushkov.wordteacher.shared.general.resource.RESOURCE_UNDEFINED_PROGRESS
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.loadResourceWithProgress
import com.aglushkov.wordteacher.shared.general.resource.onData
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.general.settings.serializable
import com.aglushkov.wordteacher.shared.general.settings.setSerializable
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.math.max
import kotlin.math.min

class ArticlesRepository(
    private val database: AppDatabase,
    private val nlpCore: NLPCore,
    private val nlpSentenceProcessor: NLPSentenceProcessor,
    private val articleSettingsProvider: () -> SettingStore,
    private val timeSource: TimeSource,
    private val isDebug: Boolean,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<List<ShortArticle>>>(Resource.Loading())

    val shortArticles: StateFlow<Resource<List<ShortArticle>>> = stateFlow

    // reading progress map
    val lastFirstVisibleItemMap: MutableStateFlow<Resource<Map<Long, Int>>> = MutableStateFlow(Resource.Loading())
    private var updateFirstVisibleItemMapJob: Job? = null

    init {
        scope.launch(Dispatchers.IO) {
            val settings = articleSettingsProvider()
            lastFirstVisibleItemMap.update {
                Resource.Loaded(settings.serializable(FIRSTITEMINDEX_STATE_KEY) ?: emptyMap<Long, Int>())
            }

            // listen changes to store
            lastFirstVisibleItemMap.collect {
                updateFirstVisibleItemMapJob?.cancel()
                updateFirstVisibleItemMapJob = launch {
                    delay(200)
                    settings.setSerializable(FIRSTITEMINDEX_STATE_KEY, lastFirstVisibleItemMap.value.data().orEmpty())
                }
            }
        }

        scope.launch(Dispatchers.IO) {
            database.articles.selectAllShortArticles().asFlow().collect {
                val result = it.executeAsList()
                Logger.v("ArticleRepository loaded ${result.size} articles")
                stateFlow.value = Resource.Loaded(result)
            }
        }
    }

    fun updateLastFirstVisibleItem(id: Long, itemIndex: Int) {
        scope.launch(Dispatchers.IO) {
            lastFirstVisibleItemMap.waitUntilLoaded()
            lastFirstVisibleItemMap.value.onData { data ->
                if (data[id] != itemIndex) {
                    lastFirstVisibleItemMap.updateLoadedData(defaultData = emptyMap()) {
                        it + (id to itemIndex)
                    }
                }
            }
        }
    }

    fun createArticle(title: String, text: String, link: String?): Flow<Resource<Article>> {
        return flow {
            Logger.v("start", "tag_createArticle")
            emit(Resource.Loading(progress = RESOURCE_UNDEFINED_PROGRESS))
            Logger.v("wait", "tag_createArticle")
            nlpCore.waitUntilInitialized()
            Logger.v("createArticleInternal", "tag_createArticle")
            createArticleInternal(title, text, link).collect {
                emit(it)
            }
            Logger.v("done", "tag_createArticle")
        }
    }

    suspend fun removeArticle(articleId: Long) = supervisorScope {
        scope.async(Dispatchers.IO) {
            removeArticleInternal(articleId)
        }.await()
    }

    private fun createArticleInternal(
        title: String,
        text: String,
        link: String?,
    ): Flow<Resource<Article>> {
       return loadResourceWithProgress(
           loader = processTextIntoArticle(text, title, link)
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
        }

        newText.append(text.substring(newTextI))

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
        title: String,
        link: String?,
    ): Flow<Pair<Float, Article?>> {
        var resultText = clearText(text)
        val cutResult = cutStylesFromText(resultText)
        resultText = cutResult.first

        val nlpCoreCopy = nlpCore.clone()
        var sentenceSpans = nlpCoreCopy.sentenceSpans(resultText)

        // update tag positions to be sentence relative
        val headers = cutResult.second.headers.toMutableList()
        var lastHeaderIndex = 0
        sentenceSpans.onEachIndexed { sI, sSpan ->
            for (hI in lastHeaderIndex until headers.size) {
                val header = headers[hI]
                if (header.end -1 >= sSpan.start && header.start < sSpan.end) {
                    header.start = max(header.start, sSpan.start) - sSpan.start

                    if (header.end > sSpan.end) {
                        // split the tag
                        headers.add(hI + 1, header.copy(start = sSpan.end, end = header.end))

                        header.end = sSpan.end - sSpan.start
                    } else {
                        header.end -= sSpan.start
                    }

                    header.sentenceIndex = sI
                    ++lastHeaderIndex
                } else if (header.end <= sSpan.start) {
                    ++lastHeaderIndex
                    continue
                } else {
                    break
                }
            }
        }

        // build paragraphs
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

        // trim sentenceSpans
        val headerMap = headers.associateBy { it.sentenceIndex }
        sentenceSpans = sentenceSpans.mapIndexed { i, span ->
            var endI = span.end
            while (endI > 0) {
                if (!charsToTrim.contains(resultText[endI-1])) {
                    break
                }
                if (endI == 1) {
                    break
                }
                --endI
            }

            var startI = span.start
            while (startI < span.end) {
                if (!charsToTrim.contains(resultText[startI])) {
                    break
                }
                if (startI == span.end-1) {
                    break
                }
                ++startI
            }

            val startTrim = startI - span.start
            val endTrim = span.end - endI

            headerMap[i]?.apply {
                start = max(0, start - startTrim)
                end = max(0, min(end - startTrim, (span.end - span.start) - startTrim - endTrim))
            }

            span.copy(startI, endI)
        }

        val style = ArticleStyle(
            paragraphs = paragraphs,
            headers = headers
        )
        val progressChannel = Channel<Pair<Float, Article?>>(UNLIMITED)
        scope.launch(Dispatchers.IO) {
            val article = database.transactionWithResult {
                val articleId = database.articles.run {
                    insert(title, timeSource.timeInMilliseconds(), link, style)
                    insertedArticleId()
                } ?: 0L

                val sentences = mutableListOf<NLPSentence>()
                val splitSentences = resultText.split(sentenceSpans)

                // TODO: process in parallel (split splitSentences by chunks and process them with await)
                splitSentences.forEachIndexed { index, s ->
                    val nlpSentence = NLPSentence(articleId, index.toLong(), s.toString())
                    nlpSentenceProcessor.process(nlpSentence, nlpCoreCopy)
                    database.sentencesNLP.insert(nlpSentence)
                    sentences += nlpSentence

                    progressChannel.trySend((index + 1).toFloat() / splitSentences.size to null)
                }

                Article(
                    articleId,
                    title,
                    timeSource.timeInMilliseconds(),
                    link = link,
                    isRead = false,
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
        //.trim()

    private val charsToTrim = setOf(
        13.toChar(),
        10.toChar(),
        9.toChar(),
        11.toChar(),
        13.toChar(),
        3.toChar(),
        ' '
    )

    private fun clearText(s: String) = s.replace("``", "\"")
        .replace("''", "\"")
        .replace(147.toChar(), '"')
        .replace(148.toChar(), '"')
        .replace(8220.toChar(), '"')
        .replace(8221.toChar(), '"')
}

class ArticleInsertException(message: String): RuntimeException(message)

private val FIRSTITEMINDEX_STATE_KEY = "articleFirstItemState"