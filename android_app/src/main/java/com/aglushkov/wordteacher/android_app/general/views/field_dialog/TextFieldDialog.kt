package com.aglushkov.wordteacher.android_app.general.views.field_dialog

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import com.aglushkov.wordteacher.android_app.databinding.DialogTextfieldBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class TextFieldDialog(
    context: Context,
    val onTextEntered: (text: String) -> Unit
): BottomSheetDialog(context) {
    lateinit var binding: DialogTextfieldBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogTextfieldBinding.inflate(layoutInflater)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        window?.attributes?.let {
            // TODO: adapt for tablets
            it.width = ViewGroup.LayoutParams.MATCH_PARENT//context.resolveThemeInt(android.R.attr.layout_width)
            it.height = ViewGroup.LayoutParams.MATCH_PARENT//context.resolveThemeInt(android.R.attr.layout_height)
            window?.attributes = it
        }

        binding.titleField.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                onTextEntered(v.text.toString())
                true
            } else {
                false
            }
        }
    }
}
