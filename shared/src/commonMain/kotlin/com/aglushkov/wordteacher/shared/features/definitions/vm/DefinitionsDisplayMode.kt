package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc

enum class DefinitionsDisplayMode {
    BySource,
    Merged;

    fun toStringDesc() = when (this) {
        BySource -> StringDesc.Resource(MR.strings.definitions_displayMode_bySource)
        Merged -> StringDesc.Resource(MR.strings.definitions_displayMode_merge)
    }
}