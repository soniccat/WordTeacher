package com.aglushkov.wordteacher.androidApp.general.extensions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

private val SharedTypedValue = TypedValue()

@ColorInt
fun Context.resolveThemeColor(@AttrRes attribute: Int) = SharedTypedValue.let {
    theme.resolveAttribute(attribute, it, true)
    it.data
}

fun Context.resolveThemeDrawable(@AttrRes attribute: Int) = SharedTypedValue.let {
    theme.resolveAttribute(attribute, it, true)
    getDrawableCompat(it.resourceId)
}

fun Context.resolveThemeStyle(@AttrRes attribute: Int) = SharedTypedValue.let {
    theme.resolveAttribute(attribute, it, true)
    it.resourceId
}

fun Context.getDrawableCompat(@DrawableRes res: Int): Drawable? = ContextCompat.getDrawable(this, res)

fun Context.getColorCompat(@ColorRes res: Int): Int = ContextCompat.getColor(this, res)

fun Context.getColorStateListCompat(@ColorRes res: Int): ColorStateList? = ContextCompat.getColorStateList(this, res)

fun Context.getLayoutInflater(): LayoutInflater = LayoutInflater.from(this)