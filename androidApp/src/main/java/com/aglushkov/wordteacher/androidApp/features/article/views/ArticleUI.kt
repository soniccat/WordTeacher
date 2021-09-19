package com.aglushkov.wordteacher.androidApp.features.article.views

import android.text.SpannableStringBuilder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.set
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.features.article.blueprints.*
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ParagraphViewItem
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.hasData

@Composable
fun ArticleUI(
    vm: ArticleVM,
    modifier: Modifier = Modifier
) {
    val data by vm.article.collectAsState()
    val paragraphs by vm.paragraphs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background),
    ) {
        TopAppBar(
            title = { Text(stringResource(id = R.string.add_article_title)) },
            actions = {
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
                    ParagraphViewItem(item, vm)
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

@Composable
fun ParagraphViewItem(
    item: BaseViewItem<*>,
    vm: ArticleVM
) = when (item) {
    is ParagraphViewItem -> ArticleParagraphView(item) {
        //vm.onTextClicked(item)
    }
    else -> {
        Text(
            text = "unknown item $item"
        )
    }
}


@Composable
private fun ArticleParagraphView(
    paragraphViewItem: ParagraphViewItem,
    onClick: () -> Unit
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
        }
    }

    var textLayoutResult by remember {
        mutableStateOf<TextLayoutResultOrNothing>(TextLayoutResultOrNothing.NothingOption)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(
                start = dimensionResource(id = R.dimen.article_horizontalPadding),
                end = dimensionResource(id = R.dimen.article_horizontalPadding)
            )
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().drawBehind {
                val v = textLayoutResult
                if (v is TextLayoutResultOrNothing.TextLayoutResultOption) {
                    val layoutResult = v.data
                    text.getStringAnnotations(0, text.length)
                        .filter {
                            it.end < text.length
                        }
                        .onEach {
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

sealed class TextLayoutResultOrNothing {
    class TextLayoutResultOption(val data: TextLayoutResult): TextLayoutResultOrNothing()
    object NothingOption: TextLayoutResultOrNothing()
}