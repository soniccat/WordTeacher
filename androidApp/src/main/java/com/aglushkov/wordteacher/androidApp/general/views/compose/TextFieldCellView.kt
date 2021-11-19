package com.aglushkov.wordteacher.androidApp.general.views.compose

import android.annotation.SuppressLint
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography


@Composable
fun TextFieldCellView(
    placeholder: String,
    textFieldValue: TextFieldValue,
    focusRequester: FocusRequester,
    onTextChanged: (text: TextFieldValue) -> Unit,
    onCreated: (text: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var focusState by remember { mutableStateOf<FocusState>(object : FocusState {
        override val hasFocus: Boolean = false
        override val isCaptured: Boolean = false
        override val isFocused: Boolean = false
    }) }

    Column(
        modifier = modifier
            .focusRequester(focusRequester)
            .fillMaxWidth()
    ) {
        TextField(
            value = textFieldValue,
            onValueChange = onTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    focusState = it
                },
            placeholder = {
                if (!focusState.isFocused) {
                    Row {
                        Icon(
                            painter = painterResource(R.drawable.ic_create_note),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                            tint = AppTypography.notePlaceholder.color
                        )
                        Text(
                            text = placeholder,
                            style = AppTypography.notePlaceholder
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onCreated(textFieldValue.text)
                }
            ),
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent
            )
        )
    }
}

interface TextFieldCellState {
    val focusRequester: FocusRequester

    fun updateTextFieldValue(value: TextFieldValue)
    @Composable fun rememberTextFieldValueState(): TextFieldValue
    @Composable fun requestFocusIfNeeded()
}

@Stable
class TextFieldCellStateImpl(
    val getValue: () -> String?
): TextFieldCellState {
    private var innerTextFieldValue = mutableStateOf(EmptyTextFieldValue)
    override val focusRequester = FocusRequester()

    override fun updateTextFieldValue(value: TextFieldValue) {
        innerTextFieldValue.value = value
    }

    @Composable
    override fun rememberTextFieldValueState(): TextFieldValue {
        val value = getValue()
        return remember(value, innerTextFieldValue.value) {
            if (value.orEmpty() != innerTextFieldValue.value.text) {
                innerTextFieldValue.value = innerTextFieldValue.value.copy(text = value.orEmpty())
                innerTextFieldValue.value
            } else {
                innerTextFieldValue.value
            }
        }
    }

    @SuppressLint("ComposableNaming")
    @Composable
    override fun requestFocusIfNeeded() {
        LaunchedEffect("new note focus") {
            if (getValue()?.isNotEmpty() == true) {
                focusRequester.requestFocus()
            }
        }
    }
}

val EmptyTextFieldValue = TextFieldValue()
