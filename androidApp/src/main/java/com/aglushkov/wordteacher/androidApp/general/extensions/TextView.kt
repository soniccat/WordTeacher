package com.aglushkov.wordteacher.androidApp.general.extensions

import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.widget.TextViewCompat

fun TextView.setTextAppearanceCompat(@StyleRes res: Int) = TextViewCompat.setTextAppearance(this, res)