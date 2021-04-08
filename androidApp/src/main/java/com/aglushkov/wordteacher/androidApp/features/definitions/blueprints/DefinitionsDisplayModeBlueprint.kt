package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.databinding.ItemDisplayModeBinding
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.extensions.getLayoutInflater
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayMode
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import javax.inject.Inject

interface DefinitionsDisplayModeBlueprintListener {
    fun onPartOfSpeechFilterClicked(item: DefinitionsDisplayModeViewItem)
    fun onPartOfSpeechFilterCloseClicked(item: DefinitionsDisplayModeViewItem)
    fun onDisplayModeChanged(mode: DefinitionsDisplayMode)
}

class DefinitionsDisplayModeBlueprint @Inject constructor (
    var listener: DefinitionsDisplayModeBlueprintListener
): Blueprint<DefinitionsDisplayModeViewHolder, DefinitionsDisplayModeViewItem> {

    override val type: Int = DefinitionsDisplayModeViewItem.Type

    // TODO: create UI in xml
    override fun createViewHolder(parent: ViewGroup): DefinitionsDisplayModeViewHolder {
        val binding = ItemDisplayModeBinding.inflate(parent.context.getLayoutInflater(), parent, false)
        return DefinitionsDisplayModeViewHolder(binding)
    }

    override fun bind(viewHolder: DefinitionsDisplayModeViewHolder, viewItem: DefinitionsDisplayModeViewItem) {
        viewHolder.bind(viewItem, listener)
    }
}

class DefinitionsDisplayModeViewHolder(
    binding: ItemDisplayModeBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        viewItem: DefinitionsDisplayModeViewItem,
        listener: DefinitionsDisplayModeBlueprintListener
    ) {
        val context = itemView.context
        val partOfSpeechChip: Chip = itemView.findViewById(R.id.definitions_partOfSpeech_chip)
        partOfSpeechChip.setOnClickListener {
            listener.onPartOfSpeechFilterClicked(viewItem)
        }
        partOfSpeechChip.setOnCloseIconClickListener {
            listener.onPartOfSpeechFilterCloseClicked(viewItem)
        }

        partOfSpeechChip.text = viewItem.partsOfSpeechFilterText.toString(context)
        partOfSpeechChip.isCloseIconVisible = viewItem.canClearPartsOfSpeechFilter

        val chipGroup: ChipGroup = itemView.findViewById(R.id.definitions_displayMode_chipGroup)
        viewItem.items.forEachIndexed { index, definitionsDisplayMode ->
            (chipGroup.getChildAt(index) as Chip).text = definitionsDisplayMode.toStringDesc().toString(context)
        }

        chipGroup.check(viewItem.selectedIndex)
        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedMode = viewItem.items[checkedId]
            listener.onDisplayModeChanged(selectedMode)
        }
    }
}