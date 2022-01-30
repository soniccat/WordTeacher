package com.aglushkov.wordteacher.androidApp.features.article.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.features.article.blueprints.*
import com.aglushkov.wordteacher.androidApp.features.definitions.views.BottomSheet
import com.aglushkov.wordteacher.androidApp.features.definitions.views.BottomSheetStates
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.androidApp.features.definitions.views.HandleUI
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.androidApp.general.views.compose.ModalSideSheet
import com.aglushkov.wordteacher.androidApp.general.views.compose.SideSheetValue
import com.aglushkov.wordteacher.androidApp.general.views.compose.rememberSideSheetState
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleAnnotation
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ParagraphViewItem
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalUnitApi
@ExperimentalMaterialApi
@Composable
fun ArticleUI(
    vm: ArticleVM,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val article by vm.article.collectAsState()
    val paragraphs by vm.paragraphs.collectAsState()
    val data = paragraphs.data()

    val sideSheetState = rememberSideSheetState(SideSheetValue.Closed)

    BoxWithConstraints {
        val swipeableState = rememberSwipeableState(BottomSheetStates.Collapsed)
        val screenHeight = constraints.maxHeight
        val halfHeight = screenHeight/2.0f

        ModalSideSheet(
            sideSheetContent = {
                Text("abc")
            },
            sideSheetState = sideSheetState
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colors.background),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = article.data()?.name ?: "",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { vm.onBackPressed() }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back_24),
                                contentDescription = null,
                                tint = LocalContentColor.current
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    sideSheetState.open()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_article_filter_24),
                                contentDescription = null,
                                tint = LocalContentColor.current
                            )
                        }
                    }
                )

                if (data != null) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            bottom = dimensionResource(id = R.dimen.article_horizontalPadding) + this@BoxWithConstraints.maxHeight / 2
                        )
                    ) {
                        items(data) { item ->
                            ParagraphViewItem(item) { sentence, offset ->
                                val isHandled = vm.onTextClicked(sentence, offset)
                                if (isHandled) {
                                    coroutineScope.launch {
                                        swipeableState.animateTo(BottomSheetStates.Expanded)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LoadingStatusView(
                        resource = paragraphs,
                        loadingText = null,
                        errorText = vm.getErrorText(paragraphs)?.resolveString(),
                        emptyText = LocalContext.current.getString(R.string.article_empty)
                    ) {
                        vm.onTryAgainClicked()
                    }
                }
            }

            BottomSheet(
                swipeableState = swipeableState,
                anchors = mapOf(
                    halfHeight to BottomSheetStates.Expanded,
                    0f to BottomSheetStates.Full,
                    screenHeight.toFloat() to BottomSheetStates.Collapsed
                ),
                sheetContent = {
                    DefinitionsUI(
                        vm = vm.definitionsVM,
                        modalModifier = Modifier.fillMaxHeight(),
                        withSearchBar = false,
                        contentHeader = {
                            HandleUI()
                        }
                    )
                }
            )
        }
    }
}

@ExperimentalUnitApi
@Composable
fun ParagraphViewItem(
    item: BaseViewItem<*>,
    onSentenceClick: (sentence: NLPSentence, offset: Int) -> Unit
) = when (item) {
    is ParagraphViewItem -> ArticleParagraphView(item, onSentenceClick)
    else -> {
        Text(
            text = "unknown item $item"
        )
    }
}


@ExperimentalUnitApi
@Composable
private fun ArticleParagraphView(
    paragraphViewItem: ParagraphViewItem,
    onSentenceClick: (sentence: NLPSentence, offset: Int) -> Unit
) {
    val text = buildAnnotatedString {
        withStyle(
            style = ParagraphStyle()
        ) {
            paragraphViewItem.items.forEachIndexed { index, sentence ->
                val annotationStartIndex = this.length
                append(sentence.text)
                addAnnotations(
                    annotationStartIndex,
                    if (paragraphViewItem.annotations.isNotEmpty()) {
                        paragraphViewItem.annotations[index]
                    } else {
                        emptyList()
                    }
                )

                append(SENTENCE_CONNECTOR)
            }
        }
    }

    var textLayoutResult by remember {
        mutableStateOf<TextLayoutResult?>(null)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = dimensionResource(id = R.dimen.article_horizontalPadding),
                start = dimensionResource(id = R.dimen.article_horizontalPadding),
                end = dimensionResource(id = R.dimen.article_horizontalPadding)
            )
    ) {
        val colors = MaterialTheme.colors
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(onSentenceClick) {
                    detectTapGestures { pos ->
                        val v = textLayoutResult
                        if (v is TextLayoutResult) {
                            textLayoutResult.let { layoutResult ->
                                val offset = v.getOffsetForPosition(pos)
                                findSentence(paragraphViewItem, offset)?.let { (sentence, offset) ->
                                    onSentenceClick(sentence, offset)
                                }
                            }
                        }
                    }
                }
                .drawBehind {
                    val v = textLayoutResult
                    if (v is TextLayoutResult) {
                        text
                            .getStringAnnotations(0, text.length)
                            .filter {
                                it.end < text.length
                            }
                            .onEach {
                                drawAnnotation(it, v, colors)
                            }

//                    data.getLineForOffset()
//
//                    val lt = data.getLineTop(0)
//                    val lb = data.getLineBottom(0)
//                    val ll = data.getLineLeft(0)
//                    val lr = data.getLineRight(0)
//                    drawRect(
//                        color = Color.Red,
//                        topLeft = Offset(ll, lt),
//                        size = Size(lr - ll, lb - lt)
//                    )
                    }
                },
            fontSize = TextUnit(16f, TextUnitType.Sp),
            style = AppTypography.wordDefinition,
            onTextLayout = {
                textLayoutResult = it
            }
        )
    }
}

private fun findSentence(viewItem: ParagraphViewItem, index: Int): Pair<NLPSentence, Int>? {
    if (viewItem.items.isEmpty()) {
        return null
    }

    var textIndex = 0
    var sentenceIndex = 0

    while (
        sentenceIndex < viewItem.items.size &&
        index > textIndex + viewItem.items[sentenceIndex].text.length
    ) {
        textIndex += viewItem.items[sentenceIndex].text.length + SENTENCE_CONNECTOR.length
        ++sentenceIndex
    }

    return if (sentenceIndex < viewItem.items.size) {
        viewItem.items[sentenceIndex] to index - textIndex
    } else {
        null
    }
}

private fun AnnotatedString.Builder.addAnnotations(
    annotationSentenceStartIndex: Int,
    annotations: List<ArticleAnnotation>
) {
    annotations.onEach { annotation ->
        when (annotation) {
            is ArticleAnnotation.LearnProgress -> {
                addStringAnnotation(
                    ROUNDED_ANNOTATION_KEY,
                    ROUNDED_ANNOTATION_PROGRESS_VALUE_PREFIX + annotation.learnLevel,
                    annotationSentenceStartIndex + annotation.start,
                    annotationSentenceStartIndex + annotation.end
                )
            }
            is ArticleAnnotation.PartOfSpeech -> {
                addStringAnnotation(
                    ROUNDED_ANNOTATION_KEY,
                    ROUNDED_ANNOTATION_PART_OF_SPEECH_VALUE_PREFIX + annotation.partOfSpeech.name,
                    annotationSentenceStartIndex + annotation.start,
                    annotationSentenceStartIndex + annotation.end
                )
            }
        }
    }
}

private fun DrawScope.drawAnnotation(
    it: AnnotatedString.Range<String>,
    layoutResult: TextLayoutResult,
    colors: Colors
) {
    val annotationColors = it.resolveColor(colors)
    val lineStart = layoutResult.getLineForOffset(it.start)
    val lineEnd = layoutResult.getLineForOffset(it.end)

    if (lineStart != lineEnd) {
        // TODO
    } else {
        val lt = layoutResult.getLineTop(lineStart)
        val lb = layoutResult.getLineBottom(lineStart)
        val ll = layoutResult.getHorizontalPosition(it.start, true)
        val lr = layoutResult.getHorizontalPosition(it.end, true)
        val bgOffset = 2.dp.toPx()
        val cornerRadius = 4.dp.toPx()

        annotationColors.bgColor?.let {
            drawRoundRect(
                color = it,
                topLeft = Offset(ll, lt).minus(Offset(bgOffset, bgOffset)),
                size = Size(lr - ll + 2 * bgOffset, lb - lt + 2 * bgOffset),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Fill,
                alpha = 0.2f
            )
        }

        annotationColors.strokeColor?.let {
            drawRoundRect(
                color = it,
                topLeft = Offset(ll, lt).minus(Offset(bgOffset, bgOffset)),
                size = Size(lr - ll + 2 * bgOffset, lb - lt + 2 * bgOffset),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(
                    width = 1.dp.toPx()
                ),
                alpha = 1.0f
            )
        }
    }
}

private fun AnnotatedString.Range<String>.resolveColor(
    colors: Colors
): AnnotationColors {
    return when {
        this.item.startsWith(ROUNDED_ANNOTATION_PROGRESS_VALUE_PREFIX) -> {
            val progressLevel = this.item.replace(ROUNDED_ANNOTATION_PROGRESS_VALUE_PREFIX, "").toIntOrNull() ?: 0
            val newAlpha = (0.1f + progressLevel * 0.1f).coerceAtMost(8.0f)
            AnnotationColors(
                bgColor = colors.secondary.copy(newAlpha),
                strokeColor = colors.secondary
            )
        }
        this.item.startsWith(ROUNDED_ANNOTATION_PART_OF_SPEECH_VALUE_PREFIX) -> {
            val partOfSpeechName = this.item.replace(ROUNDED_ANNOTATION_PART_OF_SPEECH_VALUE_PREFIX, "")
            val partOfSpeech = WordTeacherWord.PartOfSpeech.valueOf(partOfSpeechName)
            when (partOfSpeech) {
                WordTeacherWord.PartOfSpeech.Adverb -> AnnotationColors(
                    Color.Yellow,
                    Color.Red
                )
                else -> AnnotationColors(null, null)
            }
        }
        else -> AnnotationColors(null, null)
    }
}

private data class AnnotationColors(
    val bgColor: Color?,
    val strokeColor: Color?
)

private const val SENTENCE_CONNECTOR = " "