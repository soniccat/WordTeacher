package com.aglushkov.wordteacher.androidApp.features.learning_session_result.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVM
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionTermResultViewItem
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LearningSessionResultUI(
    vm: LearningSessionResultVM,
    modifier: Modifier = Modifier
) {
    val viewItemsRes by vm.viewItems.collectAsState()
    val data = viewItemsRes.data()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background),
    ) {
        TopAppBar(
            title = {
                Text(text = stringResource(id = R.string.learning_session_result_title))
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
}

@Composable
fun LearningSessionResultViewItems(
    modifier: Modifier,
    itemView: BaseViewItem<*>,
    vm: LearningSessionResultVM,
) {
    when(val item = itemView) {
        is LearningSessionTermResultViewItem -> LearningSessionCardResultView(item, modifier)
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
    modifier: Modifier = Modifier
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
            style = AppTypography.learningSessionTerm
        )
        Text(
            text = String.format("%.2f", viewItem.newProgress),
            style = AppTypography.learningSessionProgress
        )
    }
}