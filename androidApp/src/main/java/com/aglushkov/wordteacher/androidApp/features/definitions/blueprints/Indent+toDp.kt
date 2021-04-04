package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.content.res.Resources
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.shared.features.definitions.vm.Indent

fun Indent.toDp(resources: Resources) = when (this) {
    Indent.SMALL -> resources.getDimensionPixelOffset(R.dimen.indent_small)
    Indent.NONE -> 0
}
