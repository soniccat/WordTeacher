package com.aglushkov.wordteacher.androidApp.features.learning.views

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames.remember
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordDefinitionView
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordExampleView
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordPartOfSpeechView
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordSubHeaderView
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordSynonymView
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordTitleView
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.androidApp.general.views.compose.TextFieldCellState
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordExampleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSynonymViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTitleViewItem
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LearningUI(
    vm: LearningVM,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val errorString by vm.titleErrorFlow.collectAsState()
    val viewItemsRes by vm.viewItems.collectAsState()
    val data = viewItemsRes.data()
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background),
    ) {
        TopAppBar(
            title = {
                Text(text = stringResource(id = R.string.learning_title))
            },
            navigationIcon = {
                IconButton(
                    onClick = { vm.onBackPressed() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back_24),
                        contentDescription = null,
                        tint = LocalContentColor.current
                    )
                }
            }
        )

        if (data != null) {
            TermInput(
                errorString = errorString?.resolveString(),
                focusRequester = focusRequester,
                onDone = { value ->
                    vm.onCheckPressed(value)
                }
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    bottom = 300.dp
                )
            ) {
                items(data, key = { it.id }) { item ->
                    LearningViewItems(Modifier.animateItemPlacement(), item, vm)
                }
            }

            LaunchedEffect(key1 = "editing") {
                focusRequester.requestFocus()
            }
        } else {
            LoadingStatusView(
                resource = viewItemsRes,
                loadingText = null,
                errorText = vm.getErrorText(viewItemsRes)?.resolveString()
            ) {
                vm.onTryAgainClicked()
            }
        }
    }
}

@Composable
fun TermInput(
    modifier: Modifier = Modifier,
    errorString: String?,
    focusRequester: FocusRequester,
    onDone: (value: String) -> Unit,
) {
    var textValue by remember { mutableStateOf("") }
    val hasError by remember(errorString) {
        derivedStateOf { errorString != null }
    }
    val focusManager = LocalFocusManager.current

    Column(
        modifier
            .fillMaxWidth()
            .padding(
                all = dimensionResource(id = R.dimen.learning_horizontalPadding)
            )
    ) {
        TextField(
            value = textValue,
            onValueChange = { textValue = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(id = R.string.learning_term_title_hint)) },
            isError = hasError,
            trailingIcon = {
                if (hasError) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_error_24),
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onDone(textValue)
                }
            ),
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
            )
        )

        Box(
            modifier = Modifier
                .height(15.dp)
                .padding(horizontal = dimensionResource(id = R.dimen.learning_horizontalPadding))
        ) {
            if (errorString != null) {
                Text(
                    text = errorString,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
fun LearningViewItems(
    modifier: Modifier,
    itemView: BaseViewItem<*>,
    vm: LearningVM,
) {
    when(val item = itemView) {
        is WordPartOfSpeechViewItem -> WordPartOfSpeechView(item, modifier, topPadding = 0.dp)
        is WordDefinitionViewItem -> WordDefinitionView(
            item,
            modifier,
            textContent = { text, ts ->
                Text(
                    modifier = Modifier.weight(1.0f),
                    text = text,
                    style = ts
                )
            }
        )
        is WordSubHeaderViewItem -> WordSubHeaderView(item, modifier)
        is WordSynonymViewItem -> WordSynonymView(item, modifier)
        is WordExampleViewItem -> WordExampleView(item, modifier)
        else -> {
            Text(
                text = "unknown item $item",
                modifier = modifier
            )
        }
    }
}
