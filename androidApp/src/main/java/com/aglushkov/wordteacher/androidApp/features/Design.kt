package com.aglushkov.wordteacher.androidApp.features

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updatePadding
import com.aglushkov.wordteacher.androidApp.R

object Design {
    fun createTextView(parent: ViewGroup): TextView {
        val textView = TextView(parent.context)
        setTextHorizontalPadding(textView)
        return textView
    }

    fun setTextHorizontalPadding(view: View) {
        val horizontalPadding = view.resources.getDimensionPixelSize(R.dimen.word_horizontalPadding)
        view.updatePadding(left = horizontalPadding, right = horizontalPadding)
    }
}