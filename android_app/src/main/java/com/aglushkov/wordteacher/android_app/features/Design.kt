package com.aglushkov.wordteacher.android_app.features

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updatePadding
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.android_app.general.views.CustomTextView

object Design {
    fun createTextView(parent: ViewGroup): TextView {
        val textView = TextView(parent.context)
        setTextHorizontalPadding(textView)
        return textView
    }

    fun createCustomTextView(parent: ViewGroup): CustomTextView {
        val textView = CustomTextView(parent.context)
        setTextHorizontalPadding(textView)
        return textView
    }

    fun setTextHorizontalPadding(view: View) {
        val horizontalPadding = view.resources.getDimensionPixelSize(R.dimen.word_horizontalPadding)
        view.updatePadding(left = horizontalPadding, right = horizontalPadding)
    }
}