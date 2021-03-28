package com.aglushkov.wordteacher.androidApp.features.article.blueprints

import android.text.Annotation
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.set
import androidx.core.text.toSpannable
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.androidApp.general.views.RoundedBgTextView
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ParagraphViewItem
import javax.inject.Inject

class ParagraphBlueprint @Inject constructor(
    val vm: ArticleVM
): Blueprint<SimpleAdapter.ViewHolder<RoundedBgTextView>, ParagraphViewItem> {
    override val type: Int = ParagraphViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = SimpleAdapter.ViewHolder(
        RoundedBgTextView(parent.context).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordDefinitionTextAppearance))
        }
    )

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<RoundedBgTextView>, viewItem: ParagraphViewItem) {
        setGesture(viewHolder) { index ->
            handleTextTap(index, viewItem)
        }

        val spannable = viewItem.items
            .map {
                it.text
            }
            .fold("") { a, b ->
                "$a$SENTENCE_CONNECTOR$b"
            }.toSpannable()

        spannable[0..40] = Annotation("value", "rounded")
        //spannable[0..40] = RoundedBackgroundSpan(Color.CYAN, Color.BLACK, 160, 30.0f)//BackgroundColorSpan(Color.CYAN)

        viewHolder.typedView.text = spannable
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
            vm.onTextClicked(index - textIndex, sentence)
        }
    }

    private fun setGesture(
        viewHolder: SimpleAdapter.ViewHolder<RoundedBgTextView>,
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

private const val SENTENCE_CONNECTOR = " "