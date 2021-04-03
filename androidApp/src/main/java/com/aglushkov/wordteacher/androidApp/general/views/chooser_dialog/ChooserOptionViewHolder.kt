package com.aglushkov.wordteacher.androidApp.general.views.chooser_dialog

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.databinding.ItemArticleBinding
import com.aglushkov.wordteacher.androidApp.databinding.ItemChooserBinding

class ChooserOptionViewHolder(
    private var binding: ItemChooserBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(name: String, isChecked: Boolean) {
        binding.name.text = name
        binding.checkbox.isChecked = isChecked
    }
}