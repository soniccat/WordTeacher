package com.aglushkov.wordteacher.androidApp.general.views.chooser_dialog

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import com.aglushkov.wordteacher.androidApp.databinding.DialogChooserBinding
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.androidApp.general.extensions.submit
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.google.android.material.bottomsheet.BottomSheetDialog

class ChooserDialog(
    context: Context,
    onOptionSelected: (options: List<ChooserViewItem>) -> Unit
): BottomSheetDialog(context) {
    private var viewItemBinding = ViewItemBinder().addBlueprint(
        ChooserOptionBlueprint { viewItem ->
            showOptions(
                options.map {
                    if (it == viewItem) {
                        ChooserViewItem(viewItem.id, viewItem.name, viewItem.obj, !viewItem.isSelected)
                    } else {
                        it
                    }
                }
            )
            onOptionSelected(options)
        }
    )
    lateinit var binding: DialogChooserBinding
    private var options: List<ChooserViewItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogChooserBinding.inflate(layoutInflater)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        window?.attributes?.let {
            // TODO: adapt for tablets
            it.width = ViewGroup.LayoutParams.MATCH_PARENT//context.resolveThemeInt(android.R.attr.layout_width)
            it.height = ViewGroup.LayoutParams.MATCH_PARENT//context.resolveThemeInt(android.R.attr.layout_height)
            window?.attributes = it
        }

        binding.optionList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    fun showOptions(options: List<ChooserViewItem>) {
        this.options = options.toList()
        binding.optionList.submit(Resource.Loaded(this.options), viewItemBinding)
    }
}
