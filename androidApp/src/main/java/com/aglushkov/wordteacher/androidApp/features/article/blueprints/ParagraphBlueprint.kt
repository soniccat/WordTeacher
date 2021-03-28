package com.aglushkov.wordteacher.androidApp.features.article.blueprints

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ParagraphViewItem
import javax.inject.Inject

//interface ParagraphViewListener {
//    fun onParagraphClicked(index: Int)
//}

class ParagraphBlueprint @Inject constructor(
    val vm: ArticleVM
): Blueprint<SimpleAdapter.ViewHolder<TextView>, ParagraphViewItem> {
    override val type: Int = ParagraphViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = SimpleAdapter.ViewHolder(
        Design.createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordDefinitionTextAppearance))
        }
    )

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<TextView>, viewItem: ParagraphViewItem) {
        setGesture(viewHolder) { index ->
            if (viewItem.items.isEmpty()) return@setGesture

            var textIndex = 0
            var sentenceIndex = 0
            while (index > textIndex + viewItem.items[sentenceIndex].text.length) {
                ++sentenceIndex
                textIndex += viewItem.items[sentenceIndex].text.length + SENTENCE_CONNECTOR.length
            }

            if (sentenceIndex < viewItem.items.size) {
                val sentence = viewItem.items[sentenceIndex]
                vm.onTextClicked(index - textIndex, sentence)
            }
        }
        viewHolder.typedView.text = viewItem.items
            .map {
                it.text
            }
            .fold("") { a, b ->
                "$a$SENTENCE_CONNECTOR$b"
            }
    }

    private fun setGesture(
        viewHolder: SimpleAdapter.ViewHolder<TextView>,
        listener: (Int) -> Unit
    ) {
        val gestureDetector = GestureDetector(
            viewHolder.itemView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent?): Boolean {
                    return true
                }

                override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                    val x = event.x
                    val y = event.y
                    val offset = viewHolder.typedView.getOffsetForPosition(x, y)

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

//    private fun findWord(
//        str: CharSequence,
//        anOffset: Int
//    ): String {
//        var offset = anOffset
//        if (str.length == offset) {
//            offset--
//        }
//
//        if (offset > 0 && str[offset] == ' ') {
//            offset--
//        }
//
//        var startIndex = offset
//        var endIndex = offset
//
//        while (startIndex - 1 >= 0 && !isBlankChar(str[startIndex - 1])) {
//            --startIndex
//        }
//
//        while (endIndex + 1 < str.length && !isBlankChar(str[endIndex + 1])) {
//            ++endIndex
//        }
//
//        if (startIndex != endIndex && endIndex < str.length) {
//            ++endIndex
//        }
//
//        return str.substring(startIndex, endIndex)
//    }
//
//    private fun isBlankChar(char: Char): Boolean {
//        return char == ' ' || char == '\n'
//    }
}

private const val SENTENCE_CONNECTOR = " "