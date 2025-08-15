package com.aglushkov.wordteacher.shared.features.article.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.aglushkov.wordteacher.shared.di.LocalIsDarkTheme
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleAnnotation
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ParagraphViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.HintViewItem
import com.aglushkov.wordteacher.shared.features.definitions.views.BottomSheetStates
import com.aglushkov.wordteacher.shared.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.shared.features.definitions.views.HandleUI
import com.aglushkov.wordteacher.shared.general.BackHandler
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.LocalDimensWord
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.views.HintView
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.general.views.ModalSideSheet
import com.aglushkov.wordteacher.shared.general.views.SideSheetValue
import com.aglushkov.wordteacher.shared.general.views.pxToDp
import com.aglushkov.wordteacher.shared.general.views.rememberSideSheetState
import com.aglushkov.wordteacher.shared.model.Header
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.nlp.ChunkType
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.chunkEnum
import com.aglushkov.wordteacher.shared.model.nlp.toStringDesc
import com.aglushkov.wordteacher.shared.model.partOfSpeechEnum
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.desc.ResourceStringDesc
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import dev.icerock.moko.resources.compose.localized

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ArticleUI(
    vm: ArticleVM,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val state by vm.state.collectAsState()
    val article by vm.article.collectAsState()
    val paragraphs by vm.paragraphs.collectAsState()
    val data = paragraphs.data()

    val sideSheetState = rememberSideSheetState(SideSheetValue.Closed)
    val restoredLastFirstVisibleItem = remember { vm.lastFirstVisibleItem }
    val lazyColumnState = rememberLazyListState(restoredLastFirstVisibleItem)

    LaunchedEffect(lazyColumnState) {
        snapshotFlow { lazyColumnState.firstVisibleItemIndex }
            .filter { it != vm.lastFirstVisibleItem }
            .collect {
                vm.onFirstItemIndexChanged(it)
            }
    }

    val isLastItemVisible by remember {
        derivedStateOf {
            lazyColumnState.layoutInfo
                .visibleItemsInfo
                .lastOrNull()?.let { lastItem ->
                    lastItem.index == lazyColumnState.layoutInfo.totalItemsCount - 1
                }
        }
    }

    var lastDownPoint: Offset by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(
        modifier = Modifier.pointerInput(Unit){
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                awaitPointerEvent(PointerEventPass.Initial)
                // have to put this line after awaitPointerEvent, otherwise coming recomposition breaks detectTapGestures in children
                lastDownPoint = down.position
            }
        },
    ) {
        val swipeableState = rememberSwipeableState(BottomSheetStates.Collapsed)
        val screenHeight = constraints.maxHeight

        BackHandler(enabled = swipeableState.currentValue != BottomSheetStates.Collapsed) {
            coroutineScope.launch {
                swipeableState.animateTo(BottomSheetStates.Collapsed)
            }
        }
        BackHandler(enabled = sideSheetState.currentValue != SideSheetValue.Closed) {
            coroutineScope.launch {
                sideSheetState.close()
            }
        }

        ModalSideSheet(
            sideSheetContent = {
                ArticleSideSheetContent(vm, state)
            },
            sideSheetState = sideSheetState
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colors.background),
            ) {
                ArticleTopBar(
                    title = article.data()?.name ?: "",
                    onBackPressed = { vm.onBackPressed() },
                    onSideSheetPressed = {
                        coroutineScope.launch {
                            sideSheetState.open()
                        }
                    }
                )

                if (data != null) {
                    LazyColumn(
                        state = lazyColumnState,
                        contentPadding = PaddingValues(
                            bottom = LocalDimensWord.current.articleHorizontalPadding + this@BoxWithConstraints.maxHeight / 2
                        )
                    ) {
                        items(
                            data,
                            key = { it.id },
                            contentType = { it.type }
                        ) { item ->
                            ArticleViewItem(
                                vm,
                                item,
                                { sentence, offset ->
                                    vm.onTextClicked(sentence, offset)
                                },
                                { sentence, offset ->
                                    vm.onTextLongPressed(sentence, offset)
                                },
                            )
                        }
                    }
                } else {
                    LoadingStatusView(
                        resource = paragraphs,
                        loadingText = null,
                        errorText = vm.getErrorText(paragraphs)?.localized(),
                        emptyText = stringResource(MR.strings.article_empty)
                    ) {
                        vm.onTryAgainClicked()
                    }
                }
            }

            // mark as read button
            AnimatedVisibility(
                visible = isLastItemVisible ?: false,
                modifier = Modifier.align(Alignment.BottomStart),
                enter = slideInVertically(
                    initialOffsetY = { it },
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it }
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .padding(LocalDimens.current.contentPadding)
                ) {
                    Button(onClick = {
                        vm.onMarkAsReadUnreadClicked()
                    }) {
                        Text(
                            text = ResourceStringDesc(if (state.isRead) {
                                MR.strings.article_mark_as_unread
                            } else {
                                MR.strings.article_mark_as_read
                            }).localized(),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            ArticleDefinitionBottomSheet(vm, swipeableState, screenHeight)

            DropdownMenu(
                expanded = state.annotationChooserState != null,
                offset = DpOffset(lastDownPoint.x.pxToDp(), lastDownPoint.y.pxToDp() - maxHeight),
                onDismissRequest = {
                    vm.onAnnotationChooserClicked(null)
                }
            ) {
                state.annotationChooserState?.annotations?.onEachIndexed { i, word ->
                    DropdownMenuItem(
                        onClick = {
                            vm.onAnnotationChooserClicked(i)
                        }
                    ) {
                        Text(word.entry.word)
                    }
                }
            }
        }
    }
}

// TODO: consider passing prepared view items from vm instead of resolving them here
@Composable
private fun ArticleSideSheetContent(
    vm: ArticleVM,
    state: ArticleVM.InMemoryState
) {
    val dictPaths by vm.dictPaths.collectAsState()

    Text(
        modifier = Modifier.padding(all = LocalDimens.current.contentPadding),
        text = stringResource(MR.strings.article_side_sheet_title),
        style = LocalAppTypography.current.articleSideSheetSection
    )

    CheckableListItem(
        isChecked = state.selectionState.cardSetWords,
        text = stringResource(MR.strings.article_side_sheet_selection_cardset_words),
        onClicked = { vm.onCardSetWordSelectionChanged() }
    )

    Text(
        modifier = Modifier.padding(all = LocalDimens.current.contentPadding),
        text = stringResource(MR.strings.article_side_sheet_selection_dicts),
        style = LocalAppTypography.current.articleSideSheetSection
    )
    if (dictPaths.isLoaded()) {
        if (dictPaths.data()?.isNotEmpty() == true) {
            CheckableListItem(
                isChecked = state.selectionState.filterDictSingleWordEntries,
                text = stringResource(MR.strings.article_side_sheet_selection_filter_dict_single_word_entries),
                onClicked = { vm.onFilterDictSingleWordEntriesChanged() }
            )
        }

        dictPaths.data()?.onEach {
            CheckableListItem(
                isChecked = state.selectionState.dicts.contains(it),
                text = it,
                onClicked = { vm.onDictSelectionChanged(it) }
            )
        }
    } else {
        // TODO: handle other states
    }

    Text(
        modifier = Modifier.padding(all = LocalDimens.current.contentPadding),
        text = stringResource(MR.strings.article_side_sheet_selection_phrases),
        style = LocalAppTypography.current.articleSideSheetSection
    )
    ChunkType.values().onEach { chunkType ->
        CheckableListItem(
            isChecked = state.selectionState.phrases.contains(chunkType),
            text = chunkType.toStringDesc().localized(),
            onClicked = { vm.onPhraseSelectionChanged(chunkType) }
        )
    }

    Text(
        modifier = Modifier.padding(all = LocalDimens.current.contentPadding),
        text = stringResource(MR.strings.article_side_sheet_part_of_speech_title),
        style = LocalAppTypography.current.articleSideSheetSection
    )
    WordTeacherWord.PartOfSpeech.values().onEach { partOfSpeech ->
        CheckableListItem(
            isChecked = state.selectionState.partsOfSpeech.contains(partOfSpeech),
            text = partOfSpeech.toStringDesc().localized(),
            onClicked = { vm.onPartOfSpeechSelectionChanged(partOfSpeech) }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ArticleDefinitionBottomSheet(
    vm: ArticleVM,
    swipeableState: SwipeableState<BottomSheetStates>,
    screenHeight: Int
) {
    LaunchedEffect("needShowWordDefinition handler") {
        var job: Job? = null
        vm.state.collect {
            if (it.needShowWordDefinition) {
                job?.cancel()
                if (swipeableState.currentValue == BottomSheetStates.Collapsed) {
                    job = launch {
                        swipeableState.animateTo(BottomSheetStates.Expanded)
                    }
                }
                vm.onWordDefinitionShown()
            }
        }
    }

    com.aglushkov.wordteacher.shared.features.definitions.views.BottomSheet(
        swipeableState = swipeableState,
        anchors = mapOf(
            screenHeight / 2.0f to BottomSheetStates.Expanded,
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

@Composable
private fun ArticleTopBar(
    title: String,
    onBackPressed: () -> Unit,
    onSideSheetPressed: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBackPressed
            ) {
                Icon(
                    painter = painterResource(MR.images.arrow_back_24),
                    contentDescription = null,
                    tint = LocalContentColor.current
                )
            }
        },
        actions = {
            IconButton(
                onClick = onSideSheetPressed
            ) {
                Icon(
                    painter = painterResource(MR.images.article_filter_24),
                    contentDescription = null,
                    tint = LocalContentColor.current
                )
            }
        }
    )
}

@Composable
fun ArticleViewItem(
    vm: ArticleVM,
    item: BaseViewItem<*>,
    onSentenceClick: (sentence: NLPSentence, offset: Int) -> Unit,
    onSentenceLongPressed: (sentence: NLPSentence, offset: Int) -> Unit,
) = when (item) {
    is ParagraphViewItem -> ArticleParagraphView(item, onSentenceClick, onSentenceLongPressed)
    is HintViewItem -> {
        HintView(
            hintType = item.firstItem(),
            contentPadding = PaddingValues(LocalDimens.current.contentPadding),
            onHidden = { vm.onHintClicked(item.firstItem()) }
        )
    }
    else -> {
        Text(
            text = "unknown item $item"
        )
    }
}

// http://zuga.net/articles/html-heading-elements/
private val HeaderTagSizeToEmMap = mapOf(
    1 to 1.8f, // 2.0f leads to text overlapping
    2 to 1.5f,
    3 to 1.17f,
    4 to 1.0f,
    5 to 0.83f,
    6 to 0.67f,
)
private fun Header.toSpanStyle() = SpanStyle(
    fontSize = TextUnit(HeaderTagSizeToEmMap[size] ?: 1.0f, TextUnitType.Em),
    fontWeight = FontWeight.Bold,
)

@Composable
private fun ArticleParagraphView(
    paragraphViewItem: ParagraphViewItem,
    onSentenceClick: (sentence: NLPSentence, offset: Int) -> Unit,
    onSentenceLongPressed: (sentence: NLPSentence, offset: Int) -> Unit,
) {
    val density = LocalDensity.current
    val isDarkTheme = LocalIsDarkTheme.current
    val text = buildAnnotatedString {
        withStyle(
            style = ParagraphStyle()
        ) {
            paragraphViewItem.items.forEachIndexed { index, sentence ->
                val strIndex = this.length
                append(sentence.text)
                addAnnotations(
                    strIndex,
                    if (paragraphViewItem.annotations.isNotEmpty()) {
                        paragraphViewItem.annotations[index]
                    } else {
                        emptyList()
                    }
                )
                paragraphViewItem.styles.headers.filter { it.sentenceIndex == index }.onEach { style ->
                    addStyle(style.toSpanStyle(), strIndex + style.start, strIndex + style.end)
                }

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
                top = if (paragraphViewItem.isBelowHeader) {
                    8.dp
                } else {
                    if (paragraphViewItem.isHeader) {
                        24.dp
                    } else {
                        8.dp
                    }
                },
                start = LocalDimensWord.current.articleHorizontalPadding,
                end = LocalDimensWord.current.articleHorizontalPadding,
                bottom = if (paragraphViewItem.isHeader) { 0.dp } else { 8.dp }
            )
    ) {
        val colors = MaterialTheme.colors
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(onSentenceClick) {
                    detectTapGestures(
                        onLongPress = { pos ->
                            val v = textLayoutResult
                            if (v is TextLayoutResult) {
                                textLayoutResult.let { layoutResult ->
                                    val offset = v.getOffsetForPosition(pos)
                                    findSentence(paragraphViewItem, offset)?.let { (sentence, offset) ->
                                        onSentenceLongPressed(sentence, offset)
                                    }
                                }
                            }
                        }
                    ) { pos ->
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
                                drawAnnotation(it, v, colors, isDarkTheme, density)
                            }
                    }
                },
            fontSize = ArticleFontSize,
            lineHeight = 1.5.em,
            style = LocalAppTypography.current.wordDefinition.copy(
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Bottom, // align bottom to simplify annotation frame calculation
                    trim = LineHeightStyle.Trim.None
                )
            ),
            onTextLayout = {
                textLayoutResult = it
            }
        )
    }
    //Box(modifier = Modifier.fillMaxWidth().height(10.dp).background(color = Color.Black))
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
                    ROUNDED_ANNOTATION_PROGRESS_VALUE,
                    annotation.learnLevel.toString(),
                    annotationSentenceStartIndex + annotation.start,
                    annotationSentenceStartIndex + annotation.end
                )
            }
            is ArticleAnnotation.PartOfSpeech -> {
                addStringAnnotation(
                    ROUNDED_ANNOTATION_PART_OF_SPEECH_VALUE,
                    annotation.partOfSpeech.name,
                    annotationSentenceStartIndex + annotation.start,
                    annotationSentenceStartIndex + annotation.end
                )
            }
            is ArticleAnnotation.Phrase -> {
                addStringAnnotation(
                    ROUNDED_ANNOTATION_PHRASE_VALUE,
                    annotation.phrase.name,
                    annotationSentenceStartIndex + annotation.start,
                    annotationSentenceStartIndex + annotation.end
                )
            }
            is ArticleAnnotation.DictWord -> {
                addStringAnnotation(
                    ROUNDED_ANNOTATION_DICT_VALUE,
                    annotation.dict.path.toString(),
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
    colors: Colors,
    isDarkTheme: Boolean,
    density: Density
) {
    val annotationColors = it.resolveColor(colors, isDarkTheme)
    val fontHeight = articleFontHeight(density)
    val lineStart = layoutResult.getLineForOffset(it.start)
    val lineEnd = layoutResult.getLineForOffset(it.end)

    val lb = layoutResult.getLineBottom(lineStart)
    val lt = lb - fontHeight // as we use LineHeightStyle.Alignment.Bottom we can calculate the top this way
    val bgOffset = 2.dp.toPx()
    val topBgOffset = bgOffset + 2.dp.toPx() // add extra top offset to make text looking vertically aligned inside of an annotation
    val cornerRadius = 4.dp.toPx()

    if (lineStart != lineEnd) {
        // left part
        val leftPartLl = layoutResult.getHorizontalPosition(it.start, true)
        val leftPartLr = layoutResult.getLineRight(lineStart)
        val leftRect = Rect(leftPartLl - bgOffset, lt - topBgOffset, leftPartLr + bgOffset, lb + bgOffset)
        drawAnnotationPart(leftRect, cornerRadius, isLeftPart = true, annotationColors)

        // right part
        val rightPartLb = layoutResult.getLineBottom(lineEnd)
        val rightPartLt = rightPartLb - fontHeight
        val rightPartLl = layoutResult.getLineLeft(lineEnd)
        val rightPartLr = layoutResult.getHorizontalPosition(it.end, true)
        val rightRect = Rect(rightPartLl - bgOffset, rightPartLt - topBgOffset, rightPartLr + bgOffset, rightPartLb + bgOffset)
        drawAnnotationPart(rightRect, cornerRadius, isLeftPart = false, annotationColors)

    } else {
        val ll = layoutResult.getHorizontalPosition(it.start, true)
        val lr = layoutResult.getHorizontalPosition(it.end, true)

        annotationColors.bgColor?.let {
            drawRoundRect(
                color = it,
                topLeft = Offset(ll, lt).minus(Offset(bgOffset, topBgOffset)),
                size = Size(lr - ll + 2 * bgOffset, lb - lt  + topBgOffset + bgOffset),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Fill
            )
        }

        annotationColors.strokeColor?.let {
            drawRoundRect(
                color = it,
                topLeft = Offset(ll, lt).minus(Offset(bgOffset, topBgOffset)),
                size = Size(lr - ll + 2 * bgOffset, lb - lt + topBgOffset + bgOffset),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(
                    width = 1.dp.toPx()
                )
            )
        }
    }
}

private fun DrawScope.drawAnnotationPart(
    rect: Rect,
    cornerRadius: Float,
    isLeftPart: Boolean,
    annotationColors: AnnotationColors
) {
    val path = buildRoundRectPath(
        rect,
        topLeftRadius = if (isLeftPart) cornerRadius else 0f,
        bottomLeftRadius = if (isLeftPart) cornerRadius else 0f,
        topRightRadius = if (!isLeftPart) cornerRadius else 0f,
        bottomRightRadius = if (!isLeftPart) cornerRadius else 0f
    )
    annotationColors.bgColor?.let {
        drawPath(
            path = path,
            color = it,
            style = Fill
        )
    }
    annotationColors.strokeColor?.let {
        drawPath(
            path = path,
            color = it,
            style = Stroke(
                width = 1.dp.toPx()
            )
        )
    }
}

private fun buildRoundRectPath(
    rect: Rect,
    topLeftRadius: Float = 0.0f,
    topRightRadius: Float = 0.0f,
    bottomLeftRadius: Float = 0.0f,
    bottomRightRadius: Float = 0.0f
): Path {
    val path = Path()
    path.moveTo(rect.left + topLeftRadius ,rect.top);
    path.lineTo(rect.right - topRightRadius,rect.top);
    path.quadraticBezierTo(rect.right, rect.top, rect.right, rect.top + topRightRadius);
    path.lineTo(rect.right, rect.bottom - bottomRightRadius)
    path.quadraticBezierTo(rect.right, rect.bottom, rect.right - bottomRightRadius, rect.bottom)
    path.lineTo(rect.left + bottomLeftRadius, rect.bottom)
    path.quadraticBezierTo(rect.left, rect.bottom, rect.left, rect.bottom - bottomLeftRadius)
    path.lineTo(rect.left, rect.top + topLeftRadius)
    path.quadraticBezierTo(rect.left, rect.top, rect.left + topLeftRadius, rect.top)
    path.close()

    return path
}

private fun AnnotatedString.Range<String>.resolveColor(
    colors: Colors,
    isDarkTheme: Boolean
): AnnotationColors {
    return when(tag) {
        ROUNDED_ANNOTATION_PROGRESS_VALUE -> {
            val progressLevel = item.toIntOrNull() ?: 0
            val newAlpha = (0.1f + progressLevel * 0.1f).coerceAtMost(8.0f)
            AnnotationColors(colors.secondary, newAlpha)
        }
        ROUNDED_ANNOTATION_PART_OF_SPEECH_VALUE ->
            PartOfSpeechToColorMap[partOfSpeechEnum(item)] ?: AnnotationColors(null)
        ROUNDED_ANNOTATION_PHRASE_VALUE ->
            PhraseTypeToColorMap[chunkEnum(item)] ?: AnnotationColors(null)
        ROUNDED_ANNOTATION_DICT_VALUE ->
            if (isDarkTheme) {
                AnnotationColors(Color(0xB4AE61BB))
            } else {
                AnnotationColors(Color(0xB44C0A57))
            }
        else -> AnnotationColors(null)
    }
}

private val PartOfSpeechToColorMap = mapOf(
    WordTeacherWord.PartOfSpeech.Noun to AnnotationColors(Color(0xFFCE00F1)),
    WordTeacherWord.PartOfSpeech.Verb to AnnotationColors(Color(0xFFD8302B)),
    WordTeacherWord.PartOfSpeech.Adjective to AnnotationColors(Color(0xFF584383)),
    WordTeacherWord.PartOfSpeech.Adverb to AnnotationColors(Color(0xFFE06F1F)),
    WordTeacherWord.PartOfSpeech.Pronoun to AnnotationColors(Color(0xFFDF3E6D)),
    WordTeacherWord.PartOfSpeech.Preposition to AnnotationColors(Color(0xFFEDD32F)),
    WordTeacherWord.PartOfSpeech.Conjunction to AnnotationColors(Color(0xFF309449)),
    WordTeacherWord.PartOfSpeech.Interjection to AnnotationColors(Color(0xFF98F0AE)),
    WordTeacherWord.PartOfSpeech.Abbreviation to AnnotationColors(Color(0xFFBEAE13)),
    WordTeacherWord.PartOfSpeech.Exclamation to AnnotationColors(Color(0xFFE40202)),
    WordTeacherWord.PartOfSpeech.Determiner to AnnotationColors(Color(0xFF14E2B9)),
    WordTeacherWord.PartOfSpeech.Undefined to AnnotationColors(Color(0xFF5F5D5D)),
)

private val PhraseTypeToColorMap = mapOf(
    ChunkType.NP to AnnotationColors(Color(0xFFE0B6E7)),
    ChunkType.VP to AnnotationColors(Color(0xFF5D70E2)),
    ChunkType.PP to AnnotationColors(Color(0xFF81CE80)),
    ChunkType.ADJP to AnnotationColors(Color(0xFFECC13D)),
    ChunkType.ADVP to AnnotationColors(Color(0xFFF33119)),
    ChunkType.X to AnnotationColors(Color(0xFF5E0A0A)),
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CheckableListItem(
    isChecked: Boolean,
    text: String,
    onClicked: () -> Unit
) = ListItem(
    modifier = Modifier.clickable(onClick = onClicked),
    trailing = {
        Checkbox(
            checked = isChecked,
            onCheckedChange = null
        )
    },
    text = { Text(text) },
)

private class AnnotationColors(
    inputColor: Color?,
    alpha: Float = 0.2f
) {
    val bgColor: Color? = inputColor?.copy(alpha = alpha)
    val strokeColor: Color? = inputColor
}

private fun articleFontHeight(density: Density): Float {
    return with(density) {ArticleFontSize.value.sp.toPx()}
}

private const val SENTENCE_CONNECTOR = " "
private const val ROUNDED_ANNOTATION_PROGRESS_VALUE = "progress_value"
private const val ROUNDED_ANNOTATION_PART_OF_SPEECH_VALUE = "part_of_speech_value"
private const val ROUNDED_ANNOTATION_PHRASE_VALUE = "phrase_value"
private const val ROUNDED_ANNOTATION_DICT_VALUE = "dict_value"

private val ArticleFontSize = TextUnit(16f, TextUnitType.Sp)
