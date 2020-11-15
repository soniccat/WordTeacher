package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.updatePadding
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsVMWrapper
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayMode
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import javax.inject.Inject

class DefinitionsDisplayModeBlueprint @Inject constructor (
    var vmWrapper: DefinitionsVMWrapper
): Blueprint<ChipGroup, DefinitionsDisplayModeViewItem> {
    override val type: Int = DefinitionsDisplayModeViewItem.Type

    override fun createView(parent: ViewGroup): ChipGroup {
        return ChipGroup(parent.context).apply {
            val padding = context.resources.getDimensionPixelSize(R.dimen.definitions_displayMode_padding)
            updatePadding(left = padding, top = padding, right = padding)

            for (i in 0 until 2) {
                addView(createChip(context, i))
            }

            isSelectionRequired = true
            isSingleSelection = true
        }
    }

    private fun createChip(context: Context, anId: Int): Chip {
        return Chip(context).apply {
            id = anId
            isCheckable = true
        }
    }

    override fun bind(view: ChipGroup, viewItem: DefinitionsDisplayModeViewItem) {
        viewItem.items.forEachIndexed { index, definitionsDisplayMode ->
            (view.getChildAt(index) as Chip).text = definitionsDisplayMode.toStringDesc().toString(view.context)
        }

        view.check(viewItem.selectedIndex)
        view.setOnCheckedChangeListener { group, checkedId ->
            val selectedMode = viewItem.items[checkedId]
            vmWrapper.vm.onDisplayModeChanged(selectedMode)
        }
    }
}