package com.aglushkov.wordteacher.shared.model

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize


@Parcelize
data class WordTeacherDefinition(
    val definitions: List<String>,
    val examples: List<String>,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val imageUrl: String?
) : Parcelable
