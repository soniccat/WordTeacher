package com.aglushkov.wordteacher.androidApp.features.notes

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.features.articles.views.roundToMax
import com.aglushkov.wordteacher.androidApp.features.articles.views.roundToMin
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.*
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDividerViewItem
import com.aglushkov.wordteacher.shared.features.notes.vm.CreateNoteViewItem
import com.aglushkov.wordteacher.shared.features.notes.vm.NoteViewItem
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Math.min
import java.lang.StrictMath.max

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun NotesUI(vm: NotesVM, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val notes by vm.notes.collectAsState()
    var searchText by remember { mutableStateOf("") }

    val newNoteText = vm.stateFlow.collectAsState()
    val editingState = vm.editingStateFlow.collectAsState()
    val newNoteState by remember { mutableStateOf(NewNoteState(newNoteText)) }
    val notesState by remember { mutableStateOf(NotesState(editingState)) }
    val lazyColumnState = rememberLazyListState()

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
                    modifier = Modifier.fillMaxWidth(),
                    state = lazyColumnState,
                    contentPadding = PaddingValues(bottom = 300.dp)
                ) {
                    items(
                        data.size,
                        key = { data[it].id }
                    ) { index ->
                        NoteViews(scope, index, data[index], vm, notesState, newNoteState, lazyColumnState)
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
    scope: CoroutineScope,
    index: Int,
    item: BaseViewItem<*>,
    vm: NotesVM,
    notesState: NotesState,
    state: NewNoteState,
    lazyListState: LazyListState
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
    is NoteViewItem -> {
        val configuration = LocalConfiguration.current
        val isEditing = { notesState.editingNote.value.item?.id == item.id }
        NoteView(
            item,
            isEditing = isEditing(),
            rememberTextFieldValue = notesState.rememberTextFieldValueState(),
            onTextChanged = {
                if (isEditing()) {
                    notesState.updateTextFieldValue(it)
                    vm.onEditingTextChanged(it.text)
                }
            },
            onDoneEditing = {
                vm.onEditingCompleted()
            },
            onClick = {
                scope.launch {
                    lazyListState.liftCell(index, configuration)

                    notesState.updateTextFieldValue(
                        TextFieldValue(
                            text = item.text,
                            selection = TextRange(item.text.length)
                        )
                    )
                    vm.onNoteClicked(item)
                }
            },
            onDeleted = { vm.onNoteRemoved(item) },
            focusRequester = notesState.focusRequester
        )
    }
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
    onNoteCreated: (text: String) -> Unit,
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
    rememberTextFieldValue: TextFieldValue,
    onTextChanged: (text: TextFieldValue) -> Unit,
    onDoneEditing: (String) -> Unit,
    onClick: () -> Unit,
    onDeleted: () -> Unit,
    focusRequester: FocusRequester,
) {
    if (isEditing) {
        EditableCell(
            textFieldValue = rememberTextFieldValue, //notesState.rememberTextFieldValueState(),
            onTextChanged = onTextChanged,
            onDonePressed = onDoneEditing,
            aFocusRequester = focusRequester
        )

        LaunchedEffect(key1 = "editing") {
            focusRequester.requestFocus()
        }
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
        LaunchedEffect("new note focus") {
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
    val focusRequester = FocusRequester()

    fun updateTextFieldValue(value: TextFieldValue) {
        innerTextFieldValue.value = value
    }

    @Composable
    fun rememberTextFieldValueState(): TextFieldValue {
        return remember(editingNote.value, innerTextFieldValue.value) {
            if (editingNote.value.item?.text.orEmpty() != innerTextFieldValue.value.text) {
                Log.d("editing", "vm: ${editingNote.value.item?.text.orEmpty()} innerTextField: ${innerTextFieldValue.value.text}")
                innerTextFieldValue.value = innerTextFieldValue.value.copy(text = editingNote.value.item?.text.orEmpty())
                innerTextFieldValue.value
            } else {
                Log.d("editing", "innerTextField: ${innerTextFieldValue.value.text}")
                innerTextFieldValue.value
            }
        }
    }

    @SuppressLint("ComposableNaming")
    @Composable
    fun requestFocusIfNeeded() {
        LaunchedEffect("editing focus") {
            focusRequester.requestFocus()
        }
    }
}

// For a wide screen scrolls the cell at the index to the top
// For a narrow screen lift the cell a bit above the list center
suspend fun LazyListState.liftCell(
    index: Int,
    configuration: Configuration,
) {
    val topOffset = layoutInfo.viewportStartOffset
    val bottomOffset = layoutInfo.viewportEndOffset
    val isWideList = configuration.screenWidthDp > configuration.screenHeightDp
    var currentOffset = 0

    layoutInfo.visibleItemsInfo.onEach {
        if (it.index == index) {
            currentOffset = it.offset
        }
    }

    val middleOffset = topOffset + (bottomOffset - topOffset)/2.5
    val needAnimate = if (isWideList) {
        currentOffset != 0
    } else {
        currentOffset > middleOffset
    }

    if (needAnimate) {
        animateScrollBy(if (isWideList) {
            currentOffset.toFloat()
        } else {
            currentOffset.toFloat() - middleOffset.toFloat()
        })
    }
}

private val EmptyTextFieldValue = TextFieldValue()
