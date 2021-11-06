package com.aglushkov.wordteacher.androidApp.general.views.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun EditableCell(
    textFieldValue: TextFieldValue,
    onTextChanged: (text: TextFieldValue) -> Unit,
    onDonePressed: (text: String) -> Unit,
    placeholder: @Composable (() -> Unit)? = null,
    aFocusRequester: FocusRequester? = null,
    focusState: MutableState<FocusState> = remember { mutableStateOf(EmptyFocusState) }
) {
    TextField(
        value = textFieldValue,
        onValueChange = onTextChanged,
        modifier = Modifier
            .apply { aFocusRequester?.let { focusRequester(it) } }
            .fillMaxWidth()
            .onFocusChanged {
                focusState.value = it
            },
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                onDonePressed(textFieldValue.text)
            }
        ),
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.Transparent
        )
    )
}

object EmptyFocusState: FocusState {
    override val hasFocus: Boolean = false
    override val isCaptured: Boolean = false
    override val isFocused: Boolean = false
}