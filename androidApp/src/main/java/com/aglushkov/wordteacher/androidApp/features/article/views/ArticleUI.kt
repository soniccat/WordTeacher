package com.aglushkov.wordteacher.androidApp.features.article.views

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.compose.shapes
import com.aglushkov.wordteacher.androidApp.features.article.blueprints.*
import com.aglushkov.wordteacher.androidApp.features.articles.views.roundToMax
import com.aglushkov.wordteacher.androidApp.features.articles.views.roundToMin
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ParagraphViewItem
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun ArticleUI(
    vm: ArticleVM,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val paragraphs by vm.paragraphs.collectAsState()

    val swipeableState = rememberSwipeableState(
        "expanded",
        confirmStateChange = {
            Log.d("articleUI", "state $it")
            true
        }
    )
    val bottomSheetState = rememberBottomSheetState(
        initialValue = BottomSheetValue.Collapsed,
        confirmStateChange = {
            if (it == BottomSheetValue.Collapsed) {
                //swipeableState.
            }
            true
        }
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

//    val fraction = remember { mutableStateOf(0.5f) }
    val anchors = mapOf(0f to "expanded", -200f to "full", 200f to "collapsed")

    Log.d("articleUI", "offset ${swipeableState.offset.value}")

    BottomSheetScaffold(
        sheetContent = {
            DefinitionsUI(
                vm = vm.definitionsVM,
                modalModifier = Modifier.fillMaxHeight(
                    //if (swipeableState.currentValue == "expanded") fraction.value else 1.0f
                    0.5f - (swipeableState.offset.value / 100.0f).roundToMin(-0.5f).roundToMax(0.3f)
                ),
                withSearchBar = false,
                contentHeader = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .swipeable(
                                swipeableState,
                                anchors,
                                thresholds = { _, _ -> FractionalThreshold(0.1f) },
                                orientation = Orientation.Vertical
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(4.dp)
                                .background(Color.LightGray, shapes.small)
                        )
                    }
                }
            )
        },
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colors.background),
        ) {
            TopAppBar(
                title = { Text(stringResource(id = R.string.add_article_title)) },
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
                }
            )

            val data = paragraphs.data()
            if (data != null) {
                LazyColumn {
                    items(data) { item ->
                        ParagraphViewItem(item) { sentence, offset ->
                            val isHandled = vm.onTextClicked(sentence, offset)
                            if (isHandled) {
                                coroutineScope.launch {
                                    scaffoldState.bottomSheetState.expand()
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
    }
}

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


@Composable
private fun ArticleParagraphView(
    paragraphViewItem: ParagraphViewItem,
    onSentenceClick: (sentence: NLPSentence, offset: Int) -> Unit
) {
    val text = buildAnnotatedString {
        withStyle(
            style = ParagraphStyle(
                lineHeight = 25.sp
            )
        ) {
            paragraphViewItem.items.forEach { sentence ->
                val annotationStartIndex = this.length
                append(sentence.text)
                addAnnotations(annotationStartIndex, sentence)

                append(SENTENCE_CONNECTOR)
            }
        }
    }

    var textLayoutResult by remember {
        mutableStateOf<TextLayoutResultOrNothing>(TextLayoutResultOrNothing.NothingOption)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.article_horizontalPadding),
                end = dimensionResource(id = R.dimen.article_horizontalPadding)
            )
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(onSentenceClick) {
                    detectTapGestures { pos ->
                        val v = textLayoutResult
                        if (v is TextLayoutResultOrNothing.TextLayoutResultOption) {
                            textLayoutResult.let { layoutResult ->
                                val offset = v.data.getOffsetForPosition(pos)
                                findSentence(paragraphViewItem, offset)?.let { (sentence, offset) ->
                                    onSentenceClick(sentence, offset)
                                }
                            }
                        }
                    }
                }
                .drawBehind {
                    val v = textLayoutResult
                    if (v is TextLayoutResultOrNothing.TextLayoutResultOption) {
                        val layoutResult = v.data
                        text
                            .getStringAnnotations(0, text.length)
                            .filter {
                                it.end < text.length
                            }
                            .onEach {
                                drawAnnotation(it, layoutResult)
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
            style = AppTypography.wordDefinition,
            onTextLayout = {
                textLayoutResult = TextLayoutResultOrNothing.TextLayoutResultOption(it)
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

@Composable
private fun AnnotatedString.Builder.addAnnotations(
    annotationStartIndex: Int,
    sentence: NLPSentence
) {
    val tagEnums = sentence.tagEnums()
    val tokenSpans = sentence.tokenSpans
    tokenSpans.forEachIndexed { index, tokenSpan ->
        val tag = tagEnums[index]
        when {
            tag.isAdj() ->
                addStringAnnotation(
                    ROUNDED_ANNOTATION_KEY,
                    ROUNDED_ANNOTATION_VALUE_ADJECTIVE,
                    annotationStartIndex + tokenSpan.start,
                    annotationStartIndex + tokenSpan.end
                )
            tag.isAdverb() ->
                addStringAnnotation(
                    ROUNDED_ANNOTATION_KEY,
                    ROUNDED_ANNOTATION_VALUE_ADVERB,
                    annotationStartIndex + tokenSpan.start,
                    annotationStartIndex + tokenSpan.end
                )
        }
    }
}

//@Composable
private fun DrawScope.drawAnnotation(
    it: AnnotatedString.Range<String>,
    layoutResult: TextLayoutResult
) {
    val lineStart = layoutResult.getLineForOffset(it.start)
    val lineEnd = layoutResult.getLineForOffset(it.end)

    if (lineStart != lineEnd) {
        // TODO
    } else {
        val lt = layoutResult.getLineTop(lineStart)
        val lb = layoutResult.getLineBottom(lineStart)
        val ll = layoutResult.getHorizontalPosition(it.start, true)
        val lr = layoutResult.getHorizontalPosition(it.end, true)
        val bgOffset = 5.dp.toPx()
        val cornerRadius = 8.dp.toPx()

        drawRoundRect(
            color = Color.Yellow,
            topLeft = Offset(ll, lt).minus(Offset(bgOffset, bgOffset)),
            size = Size(lr - ll + 2 * bgOffset, lb - lt + 2 * bgOffset),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Fill,
            alpha = 0.2f
        )

        drawRoundRect(
            color = Color.Red,
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

sealed class TextLayoutResultOrNothing {
    class TextLayoutResultOption(val data: TextLayoutResult): TextLayoutResultOrNothing()
    object NothingOption: TextLayoutResultOrNothing()
}

private const val SENTENCE_CONNECTOR = " "