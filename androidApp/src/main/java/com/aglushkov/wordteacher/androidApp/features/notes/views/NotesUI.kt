package com.aglushkov.wordteacher.androidApp.features.notes

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.CustomTopAppBar
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.androidApp.general.views.compose.SearchView
import com.aglushkov.wordteacher.shared.features.notes.vm.CreateNoteViewItem
import com.aglushkov.wordteacher.shared.features.notes.vm.NoteViewItem
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import kotlinx.coroutines.flow.collect

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun NotesUI(vm: NotesVM, modifier: Modifier = Modifier) {
    val notes by vm.notes.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val newNoteText = vm.stateFlow.collectAsState()
    val newNoteState by remember { mutableStateOf(NewNoteState(newNoteText)) }
    val newNoteFocusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column{
            CustomTopAppBar {
                SearchView(searchText, { searchText = it }) {
                    //vm.onWordSubmitted(searchText)
                }
            }

            val data = notes.data()

            if (notes.isLoaded() && data != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        data,
                        key = { it.id }
                    ) { item ->
                        NoteViews(item, vm, newNoteState, newNoteFocusRequester)
                    }
                }
            } else {
                LoadingStatusView(
                    resource = notes,
                    loadingText = null,
                    errorText = vm.getErrorText(notes)?.resolveString()
                ) {
                    vm.onTryAgainClicked()
                }
            }
        }
    }

    LaunchedEffect(key1 = "focus") {
        if (vm.stateFlow.value.newNoteText != null) {
            newNoteFocusRequester.requestFocus()
        }
    }
}


@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
private fun NoteViews(
    item: BaseViewItem<*>,
    vm: NotesVM,
    state: NewNoteState,
    focusRequester: FocusRequester
) = when (item) {
    is CreateNoteViewItem -> {
        CreateNoteView(
            noteViewItem = item,
            textFieldValue = state.textFieldValue,
            focusRequester = focusRequester,
            onTextChanged = {
                state.textFieldValue = it
                vm.onNewNoteTextChange(it.text)
            },
            onNoteCreated = {
                vm.onNoteAdded(it)
            }
        )
    }
    is NoteViewItem -> NoteView(
        item,
        onClick = { vm.onNoteClicked(item) }
    )
    else -> {
        Text(
            text = "unknown item $item"
        )
    }
}

@Composable
private fun CreateNoteView(
    noteViewItem: CreateNoteViewItem,
    textFieldValue: TextFieldValue,
    focusRequester: FocusRequester,
    onTextChanged: (text: TextFieldValue) -> Unit,
    onNoteCreated: (text: String) -> Unit
) {
    var focusState by remember { mutableStateOf<FocusState>(object : FocusState {
        override val hasFocus: Boolean = false
        override val isCaptured: Boolean = false
        override val isFocused: Boolean = false
    }) }

    Column(
        modifier = Modifier
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
                            text = noteViewItem.placeholder.toString(LocalContext.current),
                            style = AppTypography.notePlaceholder
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onNoteCreated(textFieldValue.text)
                }
            ),
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun NoteView(
    noteViewItem: NoteViewItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .fillMaxWidth()
    ) {
        Text(
            text = noteViewItem.text,
            style = AppTypography.noteText
        )
        Text(
            text = noteViewItem.date,
            style = AppTypography.noteDate
        )
    }
}

@Stable
private class NewNoteState(
    private val vmState: State<NotesVM.State>
) {
    private var innerTextFieldValue = mutableStateOf(EmptyTextFieldValue)

    var textFieldValue: TextFieldValue
        get() {
            if (vmState.value.newNoteText.orEmpty() != innerTextFieldValue.value.text) {
                innerTextFieldValue.value = innerTextFieldValue.value.copy(text = vmState.value.newNoteText.orEmpty())
            }

            return innerTextFieldValue.value
        }
        set(value) {
            innerTextFieldValue.value = value
        }
}

private val EmptyTextFieldValue = TextFieldValue()