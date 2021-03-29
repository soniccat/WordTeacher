package com.aglushkov.wordteacher.androidApp.general.textroundedbg

import android.graphics.Canvas
import android.text.Annotation
import android.text.Layout
import android.text.Spanned
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.withTranslation
import com.aglushkov.wordteacher.androidApp.features.article.blueprints.ROUNDED_ANNOTATION_KEY
import com.aglushkov.wordteacher.androidApp.general.views.TextViewBgDrawer

class RoundedTextBgDrawer(
    var bgRendererResolver: BgRendererResolver? = null
): TextViewBgDrawer {

    override fun draw(textView: AppCompatTextView, canvas: Canvas) {
        val layout = textView.layout
        val text = textView.text

        if (text is Spanned && layout != null) {
            canvas.withTranslation(
                textView.totalPaddingLeft.toFloat(),
                textView.totalPaddingTop.toFloat()
            ) {
                draw(canvas, text, layout)
            }
        }
    }

    /**
     * Call this function during onDraw of another widget such as TextView.
     *
     * @param canvas Canvas to draw onto
     * @param text
     * @param layout Layout that contains the text
     */
    private fun draw(canvas: Canvas, text: Spanned, layout: Layout) {
        val bgRendererResolver = this.bgRendererResolver ?: return

        // ideally the calculations here should be cached since they are not cheap. However, proper
        // invalidation of the cache is required whenever anything related to text has changed.
        val spans = text.getSpans(0, text.length, Annotation::class.java)
        spans.forEach { span ->
            if (span.key.equals(ROUNDED_ANNOTATION_KEY)) {
                val spanStart = text.getSpanStart(span)
                val spanEnd = text.getSpanEnd(span)
                val startLine = layout.getLineForOffset(spanStart)
                val endLine = layout.getLineForOffset(spanEnd)

                bgRendererResolver.resolve(span, startLine == endLine)?.let { renderer ->
                    // start can be on the left or on the right depending on the language direction.
                    val startOffset = (layout.getPrimaryHorizontal(spanStart)
                            + -1 * layout.getParagraphDirection(startLine) * renderer.horizontalPadding).toInt()
                    // end can be on the left or on the right depending on the language direction.
                    val endOffset = (layout.getPrimaryHorizontal(spanEnd)
                            + layout.getParagraphDirection(endLine) * renderer.horizontalPadding).toInt()

                    renderer.draw(canvas, layout, startLine, endLine, startOffset, endOffset)
                }
            }
        }
    }
}

interface BgRendererResolver {
    fun resolve(annotation: Annotation, isSingleLine: Boolean): TextRoundedBgRenderer?
}
