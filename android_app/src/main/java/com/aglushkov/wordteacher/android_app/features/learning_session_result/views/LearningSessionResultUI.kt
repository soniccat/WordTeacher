package com.aglushkov.wordteacher.android_app.features.learning_session_result.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.android_app.general.extensions.resolveString
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVM
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionTermResultViewItem
import com.aglushkov.wordteacher.shared.general.CustomDialogUI
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.res.MR
import java.util.*

//@ExperimentalUnitApi
//@ExperimentalComposeUiApi
@Composable
fun LearningSessionResultUIDialog(
    vm: LearningSessionResultVM,
    modifier: Modifier = Modifier
) {
    CustomDialogUI(
        onDismissRequest = { vm.onCloseClicked() }
    ) {
        LearningSessionResultUI(
            vm = vm,
            modifier = modifier,
            onClose = { vm.onCloseClicked() },
            actions = {
                IconButton(
                    onClick = { vm.onCloseClicked() }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LearningSessionResultUI(
    vm: LearningSessionResultVM,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val viewItemsRes by vm.viewItems.collectAsState()
    val data = viewItemsRes.data()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colors.background),
        ) {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = MR.strings.learning_session_result_title.resourceId))
                },
                actions = actions
            )

            if (data != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        bottom = 300.dp
                    )
                ) {
                    items(data, key = { it.id }) { item ->
                        LearningSessionResultViewItems(Modifier.animateItemPlacement(), item, vm)
                    }
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

        ExtendedFloatingActionButton(
            text = {
                Text(stringResource(id = MR.strings.done.resourceId).uppercase(Locale.getDefault()))
            },
            onClick = { onClose() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(dimensionResource(id = R.dimen.content_padding)
            )
        )
    }
}

@Composable
fun LearningSessionResultViewItems(
    modifier: Modifier,
    itemView: BaseViewItem<*>,
    vm: LearningSessionResultVM,
) {
    when(val item = itemView) {
        is LearningSessionTermResultViewItem -> LearningSessionCardResultView(
            item,
            modifier,
            vm,
        )
        else -> {
            Text(
                text = "unknown item $item",
                modifier = modifier
            )
        }
    }
}

@Composable
fun LearningSessionCardResultView(
    viewItem: LearningSessionTermResultViewItem,
    modifier: Modifier = Modifier,
    vm: LearningSessionResultVM,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (viewItem.isRight) {
                    Color.Green.copy(alpha = 0.3f)
                } else {
                    Color.Red.copy(alpha = 0.3f)
                }
            )
            .clickable {
                vm.onTermClicked(viewItem)
            }
            .padding(
                all = dimensionResource(id = R.dimen.word_horizontalPadding)
            )
    ) {
        Text(
            text = viewItem.term,
            modifier = Modifier
                .weight(1.0f, fill = true)
                .padding(
                    end = dimensionResource(id = R.dimen.word_horizontalPadding),
                ),
            style = LocalAppTypography.current.learningSessionTerm
        )
        Text(
            text = String.format("%.2f", viewItem.newProgress),
            style = LocalAppTypography.current.learningSessionProgress
        )
    }
}

@Preview
@Composable
fun TestPreview() {
    LearningSessionResultUI(
        LearningSessionResultVMPreview()
    )
}