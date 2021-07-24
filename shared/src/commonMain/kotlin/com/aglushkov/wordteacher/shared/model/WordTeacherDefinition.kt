package com.aglushkov.wordteacher.shared.model

import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.statekeeper.Parcelize


@Parcelize
data class WordTeacherDefinition(
    val definitions: List<String>,
    val examples: List<String>,
    val synonyms: List<String>,
    val imageUrl: String?
) : Parcelable