package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import wordteacher.shared.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import wordteacher.shared.generated.resources.definitions_displayMode_bySource
import wordteacher.shared.generated.resources.definitions_displayMode_merge

enum class DefinitionsDisplayMode {
    BySource,
    Merged;

    fun toStringResource() = when (this) {
        BySource -> StringResourceWrapper.String(Res.string.definitions_displayMode_bySource)
        Merged -> StringResourceWrapper.String(Res.string.definitions_displayMode_merge)
    }
}