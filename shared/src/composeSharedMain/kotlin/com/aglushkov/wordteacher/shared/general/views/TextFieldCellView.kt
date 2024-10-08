package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.painterResource

@OptIn(ExperimentalComposeUiApi::class)
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
    TextField(
        value = textFieldValue,
        onValueChange = onTextChanged,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                focusState = it
            }
            .onPreviewKeyEvent {
                if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                    onCreated(textFieldValue.text)
                    true
                } else {
                    false
                }
            },
        placeholder = {
            if (!focusState.isFocused) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(MR.images.create_note),
                        null,
                        Modifier.padding(end = 8.dp),
                        LocalAppTypography.current.notePlaceholder.color
                    )
                    Text(
                        text = placeholder,
                        style = LocalAppTypography.current.notePlaceholder
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

//@Composable
//fun EditableText(
//    modifier: Modifier,
//    value: TextFieldValue,
//    placeholder: String,
//    textStyle: TextStyle,
//    isEditable: Boolean,
//    onValueChange: (TextFieldValue) -> Unit,
//) {
//    if (isEditable) {
//        TestTextField(
//            modifier = modifier,
//            value = value,
//        )
//    } else {
//        Text(
//            modifier = modifier,
//            text = value.text,
//
//        )
//    }
//}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InlineTextField(
    modifier: Modifier,
    value: TextFieldValue,
    placeholder: String,
    textStyle: TextStyle,
    focusManager: FocusManager?,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    readOnly: Boolean = false,
    onValueChange: (TextFieldValue) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.let {
          if (focusManager != null) {
              it.onPreviewKeyEvent {
                  if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                      focusManager.moveFocus(FocusDirection.Down)
                      true
                  } else {
                      false
                  }
              }
          } else {
              it
          }
        },
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions.apply {
          if (focusManager != null) {
              KeyboardActions(
                  onNext = {
                      focusManager.moveFocus(FocusDirection.Down)
                  }
              )
          }
        },
        readOnly = readOnly,
        cursorBrush = SolidColor(MaterialTheme.colors.secondary),
        decorationBox = @Composable { coreTextField ->
            Box(
                contentAlignment = Alignment.CenterStart
            ) {
                coreTextField()
                if (value.text.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium)
                    )
                }
            }
        }
    )
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
