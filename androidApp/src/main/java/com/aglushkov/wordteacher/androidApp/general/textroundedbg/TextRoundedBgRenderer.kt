package com.aglushkov.wordteacher.androidApp.general.textroundedbg

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.text.Layout
import androidx.core.content.res.getDrawableOrThrow
import androidx.core.content.withStyledAttributes
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.general.extensions.getLineBottomWithoutPadding
import com.aglushkov.wordteacher.androidApp.general.extensions.getLineTopWithoutPadding
import com.google.android.material.resources.MaterialResources.getDimensionPixelSize
import kotlin.math.max
import kotlin.math.min

/**
 * Base class for single and multi line rounded background renderers.
 *
 * @param horizontalPadding the padding to be applied to left & right of the background
 * @param verticalPadding the padding to be applied to top & bottom of the background
 */
abstract class TextRoundedBgRenderer(
    val horizontalPadding: Int,
    val verticalPadding: Int
) {

    /**
     * Draw the background that starts at the {@code startOffset} and ends at {@code endOffset}.
     *
     * @param canvas Canvas to draw onto
     * @param layout Layout that contains the text
     * @param startLine the start line for the background
     * @param endLine the end line for the background
     * @param startOffset the character offset that the background should start at
     * @param endOffset the character offset that the background should end at
     */
    abstract fun draw(
        canvas: Canvas,
        layout: Layout,
        startLine: Int,
        endLine: Int,
        startOffset: Int,
        endOffset: Int
    )

    /**
     * Get the top offset of the line and add padding into account so that there is a gap between
     * top of the background and top of the text.
     *
     * @param layout Layout object that contains the text
     * @param line line number
     */
    protected fun getLineTop(layout: Layout, line: Int): Int {
        return layout.getLineTopWithoutPadding(line) - verticalPadding
    }

    /**
     * Get the bottom offset of the line and add padding into account so that there is a gap between
     * bottom of the background and bottom of the text.
     *
     * @param layout Layout object that contains the text
     * @param line line number
     */
    protected fun getLineBottom(layout: Layout, line: Int): Int {
        return layout.getLineBottomWithoutPadding(line) + verticalPadding
    }
}

/**
 * Draws the background for text that starts and ends on the same line.
 *
 * @param horizontalPadding the padding to be applied to left & right of the background
 * @param verticalPadding the padding to be applied to top & bottom of the background
 * @param drawable the drawable used to draw the background
 */
internal class SingleLineRenderer: TextRoundedBgRenderer {
    val drawable: Drawable

    constructor(
        horizontalPadding: Int,
        verticalPadding: Int,
        drawable: Drawable
    ) : super(horizontalPadding, verticalPadding) {
        this.drawable = drawable
    }

    constructor(
        context: Context,
        style: Int
    ): super(
        context.obtainStyleAttributeInt(style, R.attr.roundedTextHorizontalPadding),
        context.obtainStyleAttributeInt(style, R.attr.roundedTextVerticalPadding)
    ) {
        var drawable: Drawable? = null
        val attrs = intArrayOf(
            R.attr.roundedTextDrawable,
        )
        context.withStyledAttributes(
            resourceId = style, attrs = attrs
        ) {
            drawable = getDrawableOrThrow(0)
        }

        this.drawable = drawable!!
    }

    override fun draw(
        canvas: Canvas,
        layout: Layout,
        startLine: Int,
        endLine: Int,
        startOffset: Int,
        endOffset: Int
    ) {
        val lineTop = getLineTop(layout, startLine)
        val lineBottom = getLineBottom(layout, startLine)
        // get min of start/end for left, and max of start/end for right since we don't
        // the language direction
        val left = min(startOffset, endOffset)
        val right = max(startOffset, endOffset)
        drawable.setBounds(left, lineTop, right, lineBottom)
        drawable.draw(canvas)
    }
}

/**
 * Draws the background for text that starts and ends on different lines.
 *
 * @param horizontalPadding the padding to be applied to left & right of the background
 * @param verticalPadding the padding to be applied to top & bottom of the background
 * @param drawableLeft the drawable used to draw left edge of the background
 * @param drawableMid the drawable used to draw for whole line
 * @param drawableRight the drawable used to draw right edge of the background
 */
internal class MultiLineRenderer: TextRoundedBgRenderer {
    val drawableLeft: Drawable
    val drawableMid: Drawable
    val drawableRight: Drawable

    constructor(
        horizontalPadding: Int,
        verticalPadding: Int,
        drawableLeft: Drawable,
        drawableMid: Drawable,
        drawableRight: Drawable
    ) : super(horizontalPadding, verticalPadding) {
        this.drawableLeft = drawableLeft
        this.drawableMid = drawableMid
        this.drawableRight = drawableRight
    }

    @SuppressLint("ResourceType")
    constructor(context: Context, style: Int) : super(
        context.obtainStyleAttributeInt(style, R.attr.roundedTextHorizontalPadding),
        context.obtainStyleAttributeInt(style, R.attr.roundedTextVerticalPadding)
    ) {
        var drawableLeft: Drawable? = null
        var drawableMid: Drawable? = null
        var drawableRight: Drawable? = null

        val attrs = intArrayOf(
            R.attr.roundedTextDrawableLeft,
            R.attr.roundedTextDrawableMid,
            R.attr.roundedTextDrawableRight
        )
        context.withStyledAttributes(
            resourceId = style, attrs = attrs
        ) {
            drawableLeft = getDrawableOrThrow(0)
            drawableMid = getDrawableOrThrow(1)
            drawableRight = getDrawableOrThrow(2)
        }

        this.drawableLeft = drawableLeft!!
        this.drawableMid = drawableMid!!
        this.drawableRight = drawableRight!!
    }

    override fun draw(
        canvas: Canvas,
        layout: Layout,
        startLine: Int,
        endLine: Int,
        startOffset: Int,
        endOffset: Int
    ) {
        // draw the first line
        val paragDir = layout.getParagraphDirection(startLine)
        val lineEndOffset = if (paragDir == Layout.DIR_RIGHT_TO_LEFT) {
            layout.getLineLeft(startLine) - horizontalPadding
        } else {
            layout.getLineRight(startLine) + horizontalPadding
        }.toInt()

        var lineBottom = getLineBottom(layout, startLine)
        var lineTop = getLineTop(layout, startLine)
        drawStart(canvas, startOffset, lineTop, lineEndOffset, lineBottom)

        // for the lines in the middle draw the mid drawable
        for (line in startLine + 1 until endLine) {
            lineTop = getLineTop(layout, line)
            lineBottom = getLineBottom(layout, line)
            drawableMid.setBounds(
                (layout.getLineLeft(line).toInt() - horizontalPadding),
                lineTop,
                (layout.getLineRight(line).toInt() + horizontalPadding),
                lineBottom
            )
            drawableMid.draw(canvas)
        }

        val lineStartOffset = if (paragDir == Layout.DIR_RIGHT_TO_LEFT) {
            layout.getLineRight(startLine) + horizontalPadding
        } else {
            layout.getLineLeft(startLine) - horizontalPadding
        }.toInt()

        // draw the last line
        lineBottom = getLineBottom(layout, endLine)
        lineTop = getLineTop(layout, endLine)

        drawEnd(canvas, lineStartOffset, lineTop, endOffset, lineBottom)
    }

    /**
     * Draw the first line of a multiline annotation. Handles LTR/RTL.
     *
     * @param canvas Canvas to draw onto
     * @param start start coordinate for the background
     * @param top top coordinate for the background
     * @param end end coordinate for the background
     * @param bottom bottom coordinate for the background
     */
    private fun drawStart(canvas: Canvas, start: Int, top: Int, end: Int, bottom: Int) {
        if (start > end) {
            drawableRight.setBounds(end, top, start, bottom)
            drawableRight.draw(canvas)
        } else {
            drawableLeft.setBounds(start, top, end, bottom)
            drawableLeft.draw(canvas)
        }
    }

    /**
     * Draw the last line of a multiline annotation. Handles LTR/RTL.
     *
     * @param canvas Canvas to draw onto
     * @param start start coordinate for the background
     * @param top top position for the background
     * @param end end coordinate for the background
     * @param bottom bottom coordinate for the background
     */
    private fun drawEnd(canvas: Canvas, start: Int, top: Int, end: Int, bottom: Int) {
        if (start > end) {
            drawableLeft.setBounds(end, top, start, bottom)
            drawableLeft.draw(canvas)
        } else {
            drawableRight.setBounds(start, top, end, bottom)
            drawableRight.draw(canvas)
        }
    }
}

// TODO: this looks bizarre...
fun Context.obtainStyleAttributeInt(style: Int, attribute: Int): Int {
    var value = 0
    val attrs = intArrayOf(attribute)
    withStyledAttributes(resourceId = style, attrs = attrs) {
        value = getDimensionPixelSize(0, 0)
    }
    return value
}