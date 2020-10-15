package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.updatePadding
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayMode
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import javax.inject.Inject

class DefinitionsDisplayModeBlueprint @Inject constructor (
    var listener: Listener?
): Blueprint<ChipGroup, DefinitionsDisplayModeViewItem> {
    override val type: Int = DefinitionsDisplayModeViewItem.Type

    override fun createView(parent: ViewGroup): ChipGroup {
        return ChipGroup(parent.context).apply {
            val byCardChip = createChip(context,
                R.id.definitions_displayMode_bySource,
                context.getString(R.string.definitions_displayMode_bySource))
            val mergedChip = createChip(context,
                R.id.definitions_displayMode_merged,
                context.getString(R.string.definitions_displayMode_merge))

            val padding = context.resources.getDimensionPixelSize(R.dimen.definitions_displayMode_padding)
            updatePadding(left = padding, top = padding, right = padding)
            addView(byCardChip)
            addView(mergedChip)

            isSelectionRequired = true
            isSingleSelection = true
        }
    }

    private fun createChip(context: Context, anId: Int, aText: String): Chip {
        return Chip(context).apply {
            id = anId
            text = aText
            isCheckable = true
        }
    }

    override fun bind(view: ChipGroup, viewItem: DefinitionsDisplayModeViewItem) {
        val chipId = when (viewItem.selected) {
            DefinitionsDisplayMode.Merged -> R.id.definitions_displayMode_merged
            else -> R.id.definitions_displayMode_bySource
        }

        view.check(chipId)
        view.setOnCheckedChangeListener { group, checkedId ->
            val mode = when (checkedId) {
                R.id.definitions_displayMode_merged -> DefinitionsDisplayMode.Merged
                else -> DefinitionsDisplayMode.BySource
            }
            listener?.onDisplayModeChanged(mode)
        }
    }

    interface Listener {
        fun onDisplayModeChanged(mode: DefinitionsDisplayMode)
    }
}