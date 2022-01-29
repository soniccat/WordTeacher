package com.aglushkov.wordteacher.androidApp.features.article.blueprints

import android.text.Annotation
import android.text.SpannableStringBuilder
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.text.set
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design.createCustomTextView
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.androidApp.general.textroundedbg.BgRendererResolver
import com.aglushkov.wordteacher.androidApp.general.textroundedbg.RoundedTextBgDrawer
import com.aglushkov.wordteacher.androidApp.general.views.CustomTextView
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ParagraphViewItem
import com.aglushkov.wordteacher.shared.model.nlp.ChunkType
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import javax.inject.Inject

class ParagraphBlueprint @Inject constructor(
    val vm: ArticleVM,
    val bgRendererResolver: BgRendererResolver,
    val nlpCore: NLPCore
): Blueprint<SimpleAdapter.ViewHolder<CustomTextView>, ParagraphViewItem> {
    override val type: Int = ParagraphViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = SimpleAdapter.ViewHolder(
        createCustomTextView(parent).apply {
            this.bgDrawer = RoundedTextBgDrawer(this@ParagraphBlueprint.bgRendererResolver)
            this.setLineSpacing(0.0f, 1.2f)
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordDefinitionTextAppearance))
        }
    )

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<CustomTextView>, viewItem: ParagraphViewItem) {
        setGesture(viewHolder) { index ->
            handleTextTap(index, viewItem)
        }

        val spannableBuilder = SpannableStringBuilder()
        viewItem.items.forEach { sentence ->
            val spanStartIndex = spannableBuilder.length
            spannableBuilder.append(sentence.text)

            val tagEnums = sentence.tagEnums()
            val tokenSpans = sentence.tokenSpans
            tokenSpans.forEachIndexed { index, tokenSpan ->
                val tag = tagEnums[index]
                when {
                    tag.isAdj() -> spannableBuilder[tokenSpan.range + spanStartIndex] =
                        RoundedBgAnnotations.Adjective.annotation.cloneWithKeySuffix("${tokenSpan.start + spanStartIndex}")
                    tag.isAdverb() -> spannableBuilder[tokenSpan.range + spanStartIndex] =
                        RoundedBgAnnotations.Adverb.annotation.cloneWithKeySuffix("${tokenSpan.start + spanStartIndex}")
                }
            }

            val chunks = nlpCore.phrases(sentence)
            chunks.forEach { phraseSpan ->
                if (phraseSpan.type != ChunkType.X) {
                    val startIndex = tokenSpans[phraseSpan.start].start
                    val endIndex = if (phraseSpan.end < tokenSpans.size) {
                        tokenSpans[phraseSpan.end].end
                    } else {
                        tokenSpans[phraseSpan.start].end
                    }
                    when (phraseSpan.type) {
                        ChunkType.ADJP -> RoundedBgAnnotations.Adjective.annotation.cloneWithKeySuffix("${startIndex + spanStartIndex}")
                        ChunkType.ADVP -> RoundedBgAnnotations.Adverb.annotation.cloneWithKeySuffix("${startIndex + spanStartIndex}")
                        ChunkType.VP -> RoundedBgAnnotations.Phrase.annotation.cloneWithKeySuffix("${startIndex + spanStartIndex}")
                        else -> null
                        //else -> RoundedBgAnnotations.Phrase.annotation.cloneWithKeySuffix("${startIndex + spanStartIndex}")
                    }?.let { span ->
                        spannableBuilder[(startIndex..endIndex) + spanStartIndex] = span
                    }
                }
            }

            spannableBuilder.append(SENTENCE_CONNECTOR)
        }

        viewHolder.typedView.text = spannableBuilder
    }

    private fun handleTextTap(
        index: Int,
        viewItem: ParagraphViewItem
    ) {
        if (viewItem.items.isEmpty()) return

        var textIndex = 0
        var sentenceIndex = 0
        while (
            sentenceIndex < viewItem.items.size &&
            index > textIndex + viewItem.items[sentenceIndex].text.length
        ) {
            textIndex += viewItem.items[sentenceIndex].text.length + SENTENCE_CONNECTOR.length
            ++sentenceIndex
        }

        if (sentenceIndex < viewItem.items.size) {
            val sentence = viewItem.items[sentenceIndex]
            vm.onTextClicked(sentence, index - textIndex)
        }
    }

    private fun setGesture(
        viewHolder: SimpleAdapter.ViewHolder<CustomTextView>,
        listener: (Int) -> Unit
    ) {
        val gestureDetector = GestureDetector(
            viewHolder.itemView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent?): Boolean {
                    return true
                }

                override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                    val offset = viewHolder.typedView.getOffsetForPosition(event.x, event.y)
                    return if (offset != -1) {
                        listener(offset)
                        true
                    } else {
                        false
                    }
                }
            }
        )

        viewHolder.typedView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
        }
    }
}


enum class RoundedBgAnnotations(val annotation: Annotation) {
    Noun(Annotation(ROUNDED_ANNOTATION_KEY, ROUNDED_ANNOTATION_VALUE_NOUN)),
    Adjective(Annotation(ROUNDED_ANNOTATION_KEY, ROUNDED_ANNOTATION_VALUE_ADJECTIVE)),
    Adverb(Annotation(ROUNDED_ANNOTATION_KEY, ROUNDED_ANNOTATION_VALUE_ADVERB)),
    Phrase(Annotation(ROUNDED_ANNOTATION_KEY, ROUNDED_ANNOTATION_VALUE_PHRASE)),
}

const val ROUNDED_ANNOTATION_KEY = "rounded"
const val ROUNDED_ANNOTATION_VALUE_NOUN = "noun"
const val ROUNDED_ANNOTATION_VALUE_ADJECTIVE = "adjective"
const val ROUNDED_ANNOTATION_VALUE_ADVERB = "adverb"
const val ROUNDED_ANNOTATION_VALUE_PHRASE = "phrase"

const val ROUNDED_ANNOTATION_PROGRESS_VALUE_PREFIX = "progress_value_"
private const val SENTENCE_CONNECTOR = " "

operator fun IntRange.plus(value: Int) = IntRange(start + value, endInclusive + value)

fun Annotation.cloneWithKeySuffix(keySuffix: String) = Annotation(key + keySuffix, value)