package com.aglushkov.wordteacher.androidApp.features.definitions

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.views.WordTitleView
import com.aglushkov.wordteacher.androidApp.general.extensions.DiffCallback
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeDrawable
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDividerViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordExampleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSynonymViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTitleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTranscriptionViewItem
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

// TODO: create a universal adapter and wrap every pair of a view item and view creator
// in a single object like blueprint
class DefinitionsAdapter(
    private val binder: DefinitionsBinder
): ListAdapter<BaseViewItem<*>, DefinitionsAdapter.ViewHolder>(BaseViewItem.DiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val lp = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
        val view = when (viewType) {
            DefinitionsDisplayModeViewItem.Type -> createWordDisplayView(parent, lp)
            // Word Card Items
            WordTitleViewItem.Type -> createTitleView(parent, lp)
            WordTranscriptionViewItem.Type -> createTranscriptionView(parent, lp)
            WordPartOfSpeechViewItem.Type -> createPartOfSpeechView(parent, lp)
            WordDefinitionViewItem.Type -> createDefinitionView(parent, lp)
            WordExampleViewItem.Type -> createExampleView(parent, lp)
            WordSynonymViewItem.Type -> createSynonymView(parent, lp)
            WordSubHeaderViewItem.Type -> createSubHeaderView(parent, lp)
            WordDividerViewItem.Type -> createDividerView(parent, lp)
            else -> throw IllegalArgumentException("Unexpected viewType: $viewType")
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DefinitionsDisplayModeViewItem -> {
                val view = holder.itemView as ChipGroup
                binder.bindDisplayMode(view, item)
            }
            // Word Card Items
            is WordTitleViewItem -> {
                val view = holder.itemView as WordTitleView
                binder.bindTitle(view, item)
            }
            is WordTranscriptionViewItem -> {
                val view = holder.itemView as TextView
                binder.bindTranscription(view, item.firstItem())
            }
            is WordPartOfSpeechViewItem -> {
                val view = holder.itemView as TextView
                binder.bindPartOfSpeech(view, item.firstItem().toString(context = view.context))
            }
            is WordDefinitionViewItem -> {
                val view = holder.itemView as TextView
                binder.bindDefinition(view, item.firstItem())
            }
            is WordExampleViewItem -> {
                val view = holder.itemView as TextView
                binder.bindExample(view, item.firstItem())
            }
            is WordSynonymViewItem -> {
                val view = holder.itemView as TextView
                binder.bindSynonym(view, item.firstItem())
            }
            is WordSubHeaderViewItem -> {
                val view = holder.itemView as TextView
                binder.bindSubHeader(view, item.firstItem().toString(context = view.context))
            }
        }
    }

    private fun createWordDisplayView(parent: ViewGroup, lp: RecyclerView.LayoutParams): View {
        val context = parent.context
        return ChipGroup(context).apply {
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
            layoutParams = lp
        }
    }

    private fun createChip(context: Context, anId: Int, aText: String): Chip {
        return Chip(context).apply {
            id = anId
            text = aText
            isCheckable = true
        }
    }

    // Word View Item

    private fun createTitleView(parent: ViewGroup, lp: RecyclerView.LayoutParams): View {
        return WordTitleView(parent.context).apply {
            layoutParams = lp
        }
    }

    private fun createTranscriptionView(parent: ViewGroup, lp: RecyclerView.LayoutParams): View {
        return createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordTranscriptionTextAppearance))
            layoutParams = lp
        }
    }

    private fun createPartOfSpeechView(parent: ViewGroup, lp: RecyclerView.LayoutParams): View {
        return createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordPartOfSpeechTextAppearance))
            lp.topMargin = context.resources.getDimensionPixelSize(R.dimen.word_partOfSpeech_topMargin)
            layoutParams = lp
        }
    }

    private fun createDefinitionView(parent: ViewGroup, lp: RecyclerView.LayoutParams): View {
        return createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordDefinitionTextAppearance))
            layoutParams = lp
        }
    }

    private fun createExampleView(parent: ViewGroup, lp: RecyclerView.LayoutParams): View {
        return createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordDefinitionTextAppearance))
            layoutParams = lp
        }
    }

    private fun createSynonymView(parent: ViewGroup, lp: RecyclerView.LayoutParams): View {
        return createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordDefinitionTextAppearance))
            layoutParams = lp
        }
    }

    private fun createSubHeaderView(parent: ViewGroup, lp: RecyclerView.LayoutParams): View {
        return createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordSubHeaderTextAppearance))
            lp.topMargin = context.resources.getDimensionPixelSize(R.dimen.word_subHeader_topMargin)
            layoutParams = lp
        }
    }

    private fun createDividerView(parent: ViewGroup, lp: RecyclerView.LayoutParams): View {
        val context = parent.context
        val view = View(context)
        view.background = context.resolveThemeDrawable(R.attr.dividerHorizontal)
        setWordHorizontalPadding(view)

        lp.height = context.resources.getDimensionPixelSize(R.dimen.word_divider_height)
        lp.topMargin = context.resources.getDimensionPixelSize(R.dimen.word_divider_topMargin)
        lp.bottomMargin = context.resources.getDimensionPixelSize(R.dimen.word_divider_bottomMargin)
        view.layoutParams = lp

        return view
    }

    private fun createTextView(parent: ViewGroup): TextView {
        val textView = TextView(parent.context)
        setWordHorizontalPadding(textView)
        return textView
    }

    companion object {
        fun setWordHorizontalPadding(view: View) {
            val horizontalPadding = view.resources.getDimensionPixelSize(R.dimen.word_horizontalPadding)
            view.updatePadding(left = horizontalPadding, right = horizontalPadding)
        }
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
    }
}