@file:OptIn(ExperimentalLayoutApi::class)

package com.aglushkov.wordteacher.android_app.features.learning.views

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.android_app.general.extensions.resolveString
import com.aglushkov.wordteacher.shared.features.definitions.views.*
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordAudioFilesViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordExampleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSynonymViewItem
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.features.learning.vm.MatchSession
import com.aglushkov.wordteacher.shared.general.CustomDialogUI
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.LocalDimensWord
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.localized
import dev.icerock.moko.resources.desc.StringDesc
import java.util.*

@Composable
fun LearningUIDialog(
    vm: LearningVM,
    modifier: Modifier = Modifier
) {
    CustomDialogUI(
        onDismissRequest = { vm.onClosePressed() }
    ) {
        val playSoundOnTypingCompletion by vm.playSoundOnTypingCompletion.collectAsState()
        val challengeState by vm.challengeState.collectAsState()
        val needShowSoundIcon by remember(challengeState) {
            derivedStateOf {
                challengeState.data() is LearningVM.Challenge.Type
            }
        }

        LearningUI(
            vm = vm,
            modifier = modifier,
            actions = {
                if (needShowSoundIcon) {
                    IconButton(
                        onClick = { vm.onPlaySoundOnTypingCompletionClicked() }
                    ) {
                        Icon(
                            painter = dev.icerock.moko.resources.compose.painterResource(
                                if (playSoundOnTypingCompletion) {
                                    MR.images.audio_24
                                } else {
                                    MR.images.audio_muted_24
                                }
                            ),
                            contentDescription = null,
                            tint = LocalContentColor.current
                        )
                    }
                }

                IconButton(
                    onClick = { vm.onClosePressed() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24),
                        contentDescription = null,
                        tint = LocalContentColor.current
                    )
                }
            }
        )
    }
}

@Composable
fun LearningUI(
    vm: LearningVM,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val challengeState by vm.challengeState.collectAsState()
    val errorString by vm.titleErrorFlow.collectAsState()
    val data = challengeState.data()
    val canShowHint by vm.canShowHint.collectAsState()
    val hintString by vm.hintString.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colors.background),
        ) {
            TopAppBar(
                title = {
                    data?.let {
                        if (data.count() != 0) {
                            Text(
                                text = stringResource(id = MR.strings.learning_title.resourceId)
                                    .format(data.index() + 1, data.count())
                            )
                        } else if (data is LearningVM.Challenge.Match) {
                            val readyMatches = data.rows.filter { it.matchPair.termSelection.hasMatch() }.size
                            Text(
                                text = stringResource(id = MR.strings.learning_title.resourceId)
                                    .format(readyMatches, data.rows.size)
                            )
                        }
                    }
                },
                actions = actions
            )

            if (data != null) {
                when(data) {
                    is LearningVM.Challenge.Test -> {
                        testChallengeUI(data, vm)
                    }
                    is LearningVM.Challenge.Type -> {
                        typeChallengeUI(data, errorString, focusRequester, vm)
                    }
                    is LearningVM.Challenge.Match -> {
                        matchChallengeUI(data, vm)
                    }
                }
            } else {
                LoadingStatusView(
                    resource = challengeState,
                    loadingText = null,
                    errorText = vm.getErrorText(challengeState)?.resolveString()
                ) {
                    vm.onTryAgainClicked()
                }
            }
        }

        if (data is LearningVM.Challenge.Type) {
            typeBottomButtons(data.audioFiles, canShowHint, snackbarHostState, hintString, vm)
        }
    }
}

@Composable
private fun typeChallengeUI(
    data: LearningVM.Challenge.Type,
    errorString: StringDesc?,
    focusRequester: FocusRequester,
    vm: LearningVM
) {
    TermInput(
        term = data.term,
        errorString = errorString?.resolveString(),
        focusRequester = focusRequester,
        onDone = { value ->
            vm.onCheckPressed(value)
        }
    )
    LaunchedEffect(key1 = "editing") {
        focusRequester.requestFocus()
    }
    termInfo(data.termViewItems)
}


@Composable
private fun BoxScope.typeBottomButtons(
    audioFilesViewItem: WordAudioFilesViewItem?,
    canShowHint: Boolean,
    snackbarHostState: SnackbarHostState,
    hintString: List<Char>,
    vm: LearningVM,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomEnd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!canShowHint && audioFilesViewItem != null) {
                WordAudioFilesView(
                    viewItem = audioFilesViewItem,
                    modifier = Modifier.weight(1.0f, true)
                        // to minimize the width when there's a single line
                        .wrapContentWidth(Alignment.Start)
                        .width(IntrinsicSize.Max)
                        .padding(
                            start = LocalDimensWord.current.wordHorizontalPadding,
                            end = LocalDimensWord.current.wordHorizontalPadding,
                        )
                        .background(
                            // chip background
                            MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                .compositeOver(MaterialTheme.colors.surface),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(4.dp),
                ) {
                    vm.onAudioFileClicked(it)
                }
            } else {
                Spacer(modifier = Modifier.weight(1.0f))
            }
            
            ExtendedFloatingActionButton(
                text = {
                    Text(
                        text = stringResource(
                            id = if (canShowHint) {
                                MR.strings.learning_show_hint.resourceId
                            } else {
                                MR.strings.learning_give_up.resourceId
                            }
                        ).uppercase(Locale.getDefault()),
                        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.content_padding))
                    )
                },
                onClick = {
                    if (canShowHint) {
                        vm.onHintAskedPressed()
                    } else {
                        vm.onGiveUpPressed()
                    }
                },
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(dimensionResource(id = R.dimen.content_padding))
            )
        }

        SnackbarHost(
            modifier = Modifier.animateContentSize(),
            hostState = snackbarHostState
        ) {
            Snackbar(snackbarData = it)
        }
    }

    LaunchedEffect(key1 = hintString) {
        if (hintString.isNotEmpty()) {
            val resultString = hintString.fold("") { str, char ->
                "$str $char"
            }
            snackbarHostState.showSnackbar(resultString)
        }
    }
}

@Composable
private fun matchChallengeUI(
    data: LearningVM.Challenge.Match,
    vm: LearningVM
) {
    val instructionString = data.instruction.localized()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            bottom = 300.dp
        )
    ) {
        items(
            listOf(instructionString) + data.rows,
            key = {
                if (it is LearningVM.Challenge.MatchRow) {
                    it.id
                } else it
            }) { item ->
            if (item is LearningVM.Challenge.MatchRow) {
                matchRowUI(
                    item,
                    { vm.onMatchTermPressed(item) },
                    { vm.onMatchExamplePressed(item)}
                )
            } else if (item is String) {
                Text(
                    text = item,
                    modifier = Modifier.padding(
                        start = LocalDimens.current.contentPadding,
                        top = LocalDimens.current.contentPadding,
                        end = LocalDimens.current.contentPadding
                    ),
                    style = LocalAppTypography.current.learningHint
                )
            }
        }
    }
}

@Composable
private fun matchRowUI(
    matchRow: LearningVM.Challenge.MatchRow,
    onTermClicked: () -> Unit,
    onExampleClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(intrinsicSize = IntrinsicSize.Max)
    ) {
        Text(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1.0f)
                .run {
                    if (!matchRow.matchPair.termSelection.isChecked) {
                        clickable(onClick = onTermClicked)
                    } else {
                        this
                    }
                }
                .background(
                    color = matchRow.termColor?.let {
                        val alpha = if (matchRow.matchPair.termSelection.hasMatch()) {
                            153
                        } else {
                            80
                        }
                        Color(it.red, it.green, it.blue, alpha)
                    } ?: Color.Transparent,
                )
                .padding(
                    start = LocalDimens.current.contentPadding,
                    top = LocalDimens.current.contentPadding,
                    end = LocalDimens.current.contentPadding / 2
                ),
            textDecoration = if (matchRow.matchPair.termSelection.isChecked) {
                TextDecoration.LineThrough
            } else {
                TextDecoration.None
            },
            text = matchRow.matchPair.term,
        )
        Text(
            modifier = Modifier
                .fillMaxHeight()
                .weight(2.0f)
                .run {
                    if (!matchRow.matchPair.exampleSelection.isChecked) {
                        clickable(onClick = onExampleClicked)
                    } else {
                        this
                    }
                }
                .background(
                    color = matchRow.exampleColor?.let {
                        val alpha = if (matchRow.matchPair.exampleSelection.hasMatch()) {
                            153
                        } else {
                            80
                        }
                        Color(it.red, it.green, it.blue, alpha)
                    } ?: Color.Transparent,
                )
                .padding(
                    start = LocalDimens.current.contentPadding / 2,
                    top = LocalDimens.current.contentPadding,
                    end = LocalDimens.current.contentPadding
                ),
            textDecoration = if (matchRow.matchPair.exampleSelection.isChecked) {
                TextDecoration.LineThrough
            } else {
                TextDecoration.None
            },
            text = matchRow.matchPair.example,
        )
    }
}

@Composable
private fun testChallengeUI(
    data: LearningVM.Challenge.Test,
    vm: LearningVM
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.learning_testOption_margin))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = data.instruction.localized(),
                    modifier = Modifier.padding(
                        start = LocalDimens.current.contentPadding,
                        top = LocalDimens.current.contentPadding,
                        end = LocalDimens.current.contentPadding
                    ),
                    style = LocalAppTypography.current.learningHint
                )
            }

            val itemPerRow = 2
            var rowCount = data.testOptions.size / itemPerRow
            if (data.testOptions.size % itemPerRow != 0) {
                rowCount += 1
            }

            for (i in 0 until rowCount) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OptionButton(vm, data.testOptions[i * itemPerRow])

                    if (i * itemPerRow + 1 < data.testOptions.size) {
                        OptionButton(vm, data.testOptions[i * itemPerRow + 1])
                    }
                }
            }
        }
    }
    termInfo(data.termViewItems)
}

@Composable
fun termInfo(termViewItems: List<BaseViewItem<*>>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            bottom = 300.dp
        )
    ) {
        items(termViewItems, key = { it.id }) { item ->
            LearningViewItems(itemView = item)
        }
    }
}

@Composable
fun RowScope.OptionButton(
    vm: LearningVM,
    option: String
) {
    Button(
        modifier = Modifier
            .weight(1.0f)
            .padding(dimensionResource(id = R.dimen.learning_testOption_margin)),
        onClick = {
            vm.onTestOptionPressed(option)
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primarySurface
        )
    ) {
        Text(
            text = option,
            color = contentColorFor(MaterialTheme.colors.primarySurface)
        )
    }
}

@Composable
fun TermInput(
    modifier: Modifier = Modifier,
    term: String,
    errorString: String?,
    focusRequester: FocusRequester,
    onDone: (value: String) -> Unit,
) {
    var textValue by remember(term) { mutableStateOf("") }
    val hasError = remember(errorString) { errorString != null }

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
            label = { Text(stringResource(id = MR.strings.learning_term_title_hint.resourceId)) },
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
                .defaultMinSize(minHeight = 16.dp)
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
    modifier: Modifier = Modifier,
    itemView: BaseViewItem<*>,
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
