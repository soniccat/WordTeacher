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

class ChooserDialog(context: Context): BottomSheetDialog(context) {
    private var viewItemBinding = ViewItemBinder().addBlueprint(
        ChooserOptionBlueprint()
    )
    lateinit var binding: DialogChooserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogChooserBinding.inflate(layoutInflater)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        window?.attributes?.let {
            it.width = ViewGroup.LayoutParams.MATCH_PARENT//context.resolveThemeInt(android.R.attr.layout_width)
            it.height = ViewGroup.LayoutParams.WRAP_CONTENT//context.resolveThemeInt(android.R.attr.layout_height)
            window?.attributes = it
        }

        binding.optionList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    fun showOptions(options: List<ChooserViewItem>) {
        binding.optionList.submit(Resource.Loaded(options), viewItemBinding)
    }
}
