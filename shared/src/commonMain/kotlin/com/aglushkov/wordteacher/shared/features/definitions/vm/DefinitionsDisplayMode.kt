package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.resources.desc.Resource
import com.aglushkov.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.res.MR

enum class DefinitionsDisplayMode {
    BySource,
    Merged;

    fun toStringDesc() = when (this) {
        BySource -> StringDesc.Resource(MR.strings.definitions_displayMode_bySource)
        Merged -> StringDesc.Resource(MR.strings.definitions_displayMode_merge)
    }
}