package com.aglushkov.wordteacher.androidApp.features.add_article.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.general.views.compose.CustomDialogUI
import com.aglushkov.wordteacher.shared.events.EmptyEvent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM

@ExperimentalComposeUiApi
@Composable
fun AddArticleUI(
    vm: AddArticleVM,
    onDismissRequest: () -> Unit
) {
    CustomDialogUI(
        onDismissRequest = onDismissRequest
    ) {
        val title by vm.title.collectAsState()
        val titleError by vm.titleErrorFlow.collectAsState(initial = null)
        val text by vm.text.collectAsState()
        val events by vm.eventFlow.collectAsState(initial = EmptyEvent)

        val scrollableState = rememberScrollState()
        var wasTitleFocused by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollableState)
                .padding(
                    top = dimensionResource(id = R.dimen.content_padding),
                    start = dimensionResource(id = R.dimen.content_padding),
                    end = dimensionResource(id = R.dimen.content_padding),
                    bottom = 88.dp,
                )
        ) {
            TextField(
                value = title,
                onValueChange = { vm.onTitleChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            wasTitleFocused = true
                        }

                        if (wasTitleFocused) {
                            vm.onTitleFocusChanged(it.hasFocus)
                        }
                    },
                label = { Text("Label") },
                isError = titleError != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                ),
                singleLine = true
            )

            Box(
                modifier = Modifier.height(30.dp)
            ) {
                val titleErrorDesc = titleError
                if (titleErrorDesc != null) {
                    Text(
                        titleErrorDesc.toString(LocalContext.current),
                        color = MaterialTheme.colors.error
                    )
                }
            }

            TextField(
                value = text,
                onValueChange = { vm.onTextChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Label2") }
            )
        }

        LaunchedEffect("focus") {
            focusRequester.requestFocus()
        }
    }
}
