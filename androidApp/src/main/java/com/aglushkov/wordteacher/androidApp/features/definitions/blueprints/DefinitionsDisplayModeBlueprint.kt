package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.updatePadding
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsVMWrapper
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.androidApp.general.extensions.getColorCompat
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import javax.inject.Inject

class DefinitionsDisplayModeBlueprint @Inject constructor (
    var vmWrapper: DefinitionsVMWrapper // TODO: use an interface
): Blueprint<SimpleAdapter.ViewHolder<ViewGroup>, DefinitionsDisplayModeViewItem> {

    override val type: Int = DefinitionsDisplayModeViewItem.Type

    // TODO: create UI in xml
    override fun createViewHolder(parent: ViewGroup): SimpleAdapter.ViewHolder<ViewGroup> {
        val context = parent.context
        val container = LinearLayout(context)
        container.orientation = LinearLayout.HORIZONTAL

        val horizontalPadding = context.resources.getDimensionPixelSize(R.dimen.definitions_displayMode_horizontal_padding)
        val verticalPadding = context.resources.getDimensionPixelSize(R.dimen.definitions_displayMode_vertical_padding)
        container.updatePadding(left = horizontalPadding, top = verticalPadding, right = horizontalPadding)

        val partOfSpeechChip = createChip(context, 0, false)
        partOfSpeechChip.id = R.id.definitions_partOfSpeech_chip
        partOfSpeechChip.setChipBackgroundColorResource(R.color.partOfSpeechChipBgColor)
        partOfSpeechChip.setTextColor(context.getColorCompat(R.color.partOfSpeechChipTextColor))
        partOfSpeechChip.setCloseIconTintResource(R.color.partOfSpeechChipTextColor)
        container.addView(partOfSpeechChip)

        val displayModeChipGroup = ChipGroup(context).apply {
            for (i in 0 until 2) {
                addView(createChip(context, i, true))
            }

            isSelectionRequired = true
            isSingleSelection = true
        }
        displayModeChipGroup.id = R.id.definitions_displayMode_chipGroup
        displayModeChipGroup.updatePadding(left = horizontalPadding)
        container.addView(displayModeChipGroup)

        return SimpleAdapter.ViewHolder(container)
    }

    private fun createChip(context: Context, anId: Int, checkable: Boolean): Chip {
        return Chip(context).apply {
            id = anId
            isCheckable = checkable
        }
    }

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<ViewGroup>, viewItem: DefinitionsDisplayModeViewItem) {
        val context = viewHolder.itemView.context
        val partOfSpeechChip: Chip = viewHolder.itemView.findViewById(R.id.definitions_partOfSpeech_chip)
        partOfSpeechChip.setOnClickListener {
            vmWrapper.vm.onPartOfSpeechFilterClicked(viewItem)
        }
        partOfSpeechChip.setOnCloseIconClickListener {
            vmWrapper.vm.onPartOfSpeechFilterCloseClicked(viewItem)
        }
        partOfSpeechChip.text = viewItem.partsOfSpeechFilterText.toString(context)
        partOfSpeechChip.isCloseIconVisible = viewItem.canClearPartsOfSpeechFilter

        val chipGroup: ChipGroup = viewHolder.itemView.findViewById(R.id.definitions_displayMode_chipGroup)
        viewItem.items.forEachIndexed { index, definitionsDisplayMode ->
            (chipGroup.getChildAt(index) as Chip).text = definitionsDisplayMode.toStringDesc().toString(context)
        }

        chipGroup.check(viewItem.selectedIndex)
        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedMode = viewItem.items[checkedId]
            vmWrapper.vm.onDisplayModeChanged(selectedMode)
        }
    }
}
