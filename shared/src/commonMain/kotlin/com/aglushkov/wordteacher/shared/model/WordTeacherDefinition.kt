package com.aglushkov.wordteacher.shared.model

import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.parcelize.Parcelize


@Parcelize
data class WordTeacherDefinition(
    val definitions: List<String>,
    val examples: List<String>,
    val synonyms: List<String>,
    val imageUrl: String?
) : Parcelable