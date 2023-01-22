package com.aglushkov.wordteacher.android_app.general.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class CustomTextView : AppCompatTextView {
    var bgDrawer: TextViewBgDrawer? = null

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle
    ) : super(context, attrs, defStyleAttr) {
//        val attributeReader = TextRoundedBgAttributeReader(context, attrs)
//        textRoundedBgDrawer = TextRoundedBgDrawer(
//            horizontalPadding = attributeReader.horizontalPadding,
//            verticalPadding = attributeReader.verticalPadding,
//            drawable = attributeReader.drawable,
//            drawableLeft = attributeReader.drawableLeft,
//            drawableMid = attributeReader.drawableMid,
//            drawableRight = attributeReader.drawableRight
//        )
    }

    override fun onDraw(canvas: Canvas) {
        bgDrawer?.draw(this, canvas)
        super.onDraw(canvas)
    }
}

interface TextViewBgDrawer {
    fun draw(textView: AppCompatTextView, canvas: Canvas)
}
