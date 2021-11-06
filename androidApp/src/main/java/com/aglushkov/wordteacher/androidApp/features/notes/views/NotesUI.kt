package com.aglushkov.wordteacher.androidApp.features.notes

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import com.aglushkov.wordteacher.androidApp.general.views.compose.*
import com.aglushkov.wordteacher.shared.features.notes.vm.CreateNoteViewItem
import com.aglushkov.wordteacher.shared.features.notes.vm.NoteViewItem
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.isLoaded

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun NotesUI(vm: NotesVM, modifier: Modifier = Modifier) {
    val notes by vm.notes.collectAsState()
    var searchText by remember { mutableStateOf("") }

    val newNoteText = vm.stateFlow.collectAsState()
    val editingState = vm.editingStateFlow.collectAsState()
    val newNoteState by remember { mutableStateOf(NewNoteState(newNoteText)) }
    val notesState by remember { mutableStateOf(NotesState(editingState)) }

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
                        NoteViews(item, vm, notesState, newNoteState)
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
}

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
private fun NoteViews(
    item: BaseViewItem<*>,
    vm: NotesVM,
    notesState: NotesState,
    state: NewNoteState
) = when (item) {
    is CreateNoteViewItem -> {
        CreateNoteView(
            noteViewItem = item,
            textFieldValue = state.rememberTextFieldValueState(),
            focusRequester = state.focusRequester,
            onTextChanged = {
                state.updateTextFieldValue(it)  // update UI text field state
                vm.onNewNoteTextChange(it.text) // update VM text state
            },
            onNoteCreated = {
                vm.onNoteAdded(it)
            }
        )

        state.requestFocusIfNeeded()
    }
    is NoteViewItem -> NoteView(
        item,
        isEditing = notesState.editingNote.value.item?.id == item.id,
        rememberTextFieldValue = { notesState.rememberTextFieldValueState() },
        onTextChanged = {
            notesState.updateTextFieldValue(it)
            vm.onEditingTextChanged(it.text)
        },
        onDoneEditing = {
            vm.onEditingCompleted()
        },
        onClick = { vm.onNoteClicked(item) },
        onDeleted = { vm.onNoteRemoved(item) }
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

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
private fun NoteView(
    noteViewItem: NoteViewItem,
    isEditing: Boolean,
    rememberTextFieldValue: @Composable () -> TextFieldValue,
    onTextChanged: (text: TextFieldValue) -> Unit,
    onDoneEditing: (String) -> Unit,
    onClick: () -> Unit,
    onDeleted: () -> Unit,
) {
    if (isEditing) {
        EditableCell(
            textFieldValue = rememberTextFieldValue(), //notesState.rememberTextFieldValueState(),
            onTextChanged = onTextChanged,
            onDonePressed = onDoneEditing
        )
    } else {
        DeletableCell(
            onClick,
            onDeleted
        ) {
            Column(
                modifier = Modifier
                    .clickable {
                        onClick()
                    }
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(id = R.dimen.note_horizontalPadding),
                        end = dimensionResource(id = R.dimen.note_horizontalPadding)
                    )
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
    }
}

@Stable
class NewNoteState(
    private val vmState: State<NotesVM.State>
) {
    private var innerTextFieldValue = mutableStateOf(EmptyTextFieldValue)
    val focusRequester = FocusRequester()

    fun updateTextFieldValue(value: TextFieldValue) {
        innerTextFieldValue.value = value
    }

    @Composable
    fun rememberTextFieldValueState(): TextFieldValue {
        return remember(vmState.value, innerTextFieldValue.value) {
            if (vmState.value.newNoteText.orEmpty() != innerTextFieldValue.value.text) {
                innerTextFieldValue.value = innerTextFieldValue.value.copy(text = vmState.value.newNoteText.orEmpty())
                innerTextFieldValue.value
            } else {
                innerTextFieldValue.value
            }
        }
    }

    @SuppressLint("ComposableNaming")
    @Composable
    fun requestFocusIfNeeded() {
        LaunchedEffect("focus") {
            if (vmState.value.newNoteText?.isNotEmpty() == true) {
                focusRequester.requestFocus()
            }
        }
    }
}

@Stable
class NotesState(
    val editingNote: State<NotesVM.EditingState>
) {
    private var innerTextFieldValue = mutableStateOf(EmptyTextFieldValue)

    fun updateTextFieldValue(value: TextFieldValue) {
        innerTextFieldValue.value = value
    }

    @Composable
    fun rememberTextFieldValueState(): TextFieldValue {
        return remember(editingNote.value, innerTextFieldValue.value) {
            if (editingNote.value.item?.text.orEmpty() != innerTextFieldValue.value.text) {
                innerTextFieldValue.value = innerTextFieldValue.value.copy(text = editingNote.value.item?.text.orEmpty())
                innerTextFieldValue.value
            } else {
                innerTextFieldValue.value
            }
        }
    }
}


private val EmptyTextFieldValue = TextFieldValue()
