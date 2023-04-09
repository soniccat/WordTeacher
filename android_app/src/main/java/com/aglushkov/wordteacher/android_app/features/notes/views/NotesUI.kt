package com.aglushkov.wordteacher.android_app.features.notes

import android.content.res.Configuration
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.android_app.compose.AppTypography
import com.aglushkov.wordteacher.android_app.general.extensions.resolveString
import com.aglushkov.wordteacher.android_app.general.extensions.toStableResource
import com.aglushkov.wordteacher.android_app.general.views.compose.*
import com.aglushkov.wordteacher.shared.features.notes.vm.CreateNoteViewItem
import com.aglushkov.wordteacher.shared.features.notes.vm.NoteViewItem
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun NotesUI(vm: NotesVM, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val notes by vm.notes.collectAsState()
    var searchText by remember { mutableStateOf("") }

    val newNoteTextState = vm.stateFlow.collectAsState()
    val editingState = vm.editingStateFlow.collectAsState()
    val newNoteState by remember { mutableStateOf(TextFieldCellStateImpl { newNoteTextState.value.newNoteText }) }
    val notesState by remember { mutableStateOf(NotesState(editingState)) }
    val lazyColumnState = rememberLazyListState()

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column{
            CustomTopAppBar {
                SearchView(searchText, onTextChanged = { searchText = it }) {
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
                    resource = notes.toStableResource(),
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
    state: TextFieldCellState,
    lazyListState: LazyListState
) = when (item) {
    is CreateNoteViewItem -> {
        TextFieldCellView(
            placeholder = item.placeholder.toString(LocalContext.current),
            textFieldValue = state.rememberTextFieldValueState(),
            focusRequester = state.focusRequester,
            onTextChanged = {
                state.updateTextFieldValue(it)  // update UI text field state
                vm.onNewNoteTextChange(it.text) // update VM text state
            },
            onCreated = {
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
            modifier = Modifier.padding(
                bottom = dimensionResource(id = R.dimen.note_verticalPadding)
            ),
            textFieldValue = rememberTextFieldValue,
            onTextChanged = onTextChanged,
            onDonePressed = onDoneEditing,
            aFocusRequester = focusRequester
        )

        LaunchedEffect(key1 = "editing") {
            focusRequester.requestFocus()
        }
    } else {
        DeletableCell(
            stateKey = noteViewItem.id,
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
                        top = dimensionResource(id = R.dimen.note_verticalPadding),
                        bottom = dimensionResource(id = R.dimen.note_verticalPadding),
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

// TODO: replace with NewCellState
@Stable
class NotesState(
    val editingNote: State<NotesVM.EditingState>,
    private val textFieldState: TextFieldCellState = TextFieldCellStateImpl { editingNote.value.item?.text }
): TextFieldCellState by textFieldState {
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
