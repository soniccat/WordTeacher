package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsVMWrapper
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import javax.inject.Inject

class DefinitionsDisplayModeBlueprint @Inject constructor (
    var vmWrapper: DefinitionsVMWrapper
): Blueprint<SimpleAdapter.ViewHolder<ChipGroup>, DefinitionsDisplayModeViewItem> {

    override val type: Int = DefinitionsDisplayModeViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = SimpleAdapter.ViewHolder(
        ChipGroup(parent.context).apply {
            val horizontalPadding = context.resources.getDimensionPixelSize(R.dimen.definitions_displayMode_horizontal_padding)
            val verticalPadding = context.resources.getDimensionPixelSize(R.dimen.definitions_displayMode_vertical_padding)
            updatePadding(left = horizontalPadding, top = verticalPadding, right = horizontalPadding)

            for (i in 0 until 2) {
                addView(createChip(context, i))
            }

            isSelectionRequired = true
            isSingleSelection = true
        }
    )

    private fun createChip(context: Context, anId: Int): Chip {
        return Chip(context).apply {
            id = anId
            isCheckable = true
        }
    }

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<ChipGroup>, viewItem: DefinitionsDisplayModeViewItem) {
        val view = viewHolder.typedView
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