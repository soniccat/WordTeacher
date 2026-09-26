@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class, ExperimentalMaterialApi::class
)

package com.aglushkov.wordteacher.shared.features.find_article

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.contentColorFor
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.features.add_article.views.AddArticleUI
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.articles.views.ArticleTitleView
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.views.CardSetItemView
import com.aglushkov.wordteacher.shared.features.cardsets.views.CardSetSearchItemView
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.RemoteCardSetViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardCardSetTagsViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardCategoriesViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardExpandViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardHeadlineViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardOpenCardSetsItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardTryAgainViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardVM
import com.aglushkov.wordteacher.shared.features.dashboard.vm.HintViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordLoadingViewItem
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsViewTitleItem
import com.aglushkov.wordteacher.shared.general.CustomDialogUI
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.settings.HintType
import com.aglushkov.wordteacher.shared.general.toAnnotatedString
import com.aglushkov.wordteacher.shared.general.views.CustomAnnotatedTextListItem
import com.aglushkov.wordteacher.shared.general.views.DownloadForOfflineButton
import com.aglushkov.wordteacher.shared.general.views.HintView
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.general.views.StartLearningButton
import com.aglushkov.wordteacher.shared.general.views.windowInsetsHorizontalPadding
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.desc.ResourceStringDesc
import dev.icerock.moko.resources.compose.localized
import dev.icerock.moko.resources.compose.painterResource
import kotlinx.coroutines.launch

@Composable
fun FindArticleUIDialog(
    vm: FindArticleVM,
    modifier: Modifier = Modifier,
) {
    CustomDialogUI(
        onDismissRequest = { vm.onClosed() }
    ) {
        FindArticleUI(
            vm = vm,
            modifier = modifier,
            actions = {
                IconButton(
                    onClick = { vm.onClosed() }
                ) {
                    Icon(
                        painter = painterResource(MR.images.close_24),
                        contentDescription = null,
                        tint = LocalContentColor.current
                    )
                }
            },
        )
    }
}

@Composable
fun FindArticleUI(
    vm: FindArticleVM,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val itemsState by vm.viewItems.collectAsState()
    val items = itemsState.data()

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column {
            TopAppBar(
                title = { Text(stringResource(MR.strings.find_article_title)) },
                actions = actions
            )

            if (items?.isNotEmpty() == true) {
                val listState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().windowInsetsHorizontalPadding(),
                    state = listState,
                    contentPadding = PaddingValues(
                        bottom = 100.dp
                    )
                ) {
                    if (items.isNotEmpty()) {
                        val categoryItem = items.first()
                        stickyHeader(
                            key = categoryItem.id,
                            contentType = categoryItem.type,
                        ) {
                            findArticleItem(
                                Modifier.animateItem(),
                                categoryItem,
                                vm,
                                onCategoryChanged = {
                                    coroutineScope.launch {
                                        listState.scrollToItem(0)
                                    }
                                }
                            )
                        }
                    }

                    if (items.size > 1) {
                        items(
                            items = items.subList(1, items.size),
                            key = { it.id },
                            contentType = { it.type }
                        ) { item ->
                            findArticleItem(
                                Modifier.animateItem(),
                                item,
                                vm,
                            )
                        }
                    }
                }
            } else {
                LoadingStatusView(
                    modifier = modifier,
                    resource = itemsState,
                    errorText = vm.getErrorText(itemsState)?.localized()
                )
            }
        }
    }
}



@Composable
fun findArticleItem(
    modifier: Modifier,
    item: BaseViewItem<*>,
    vm: FindArticleVM,
    onCategoryChanged: (()->Unit)? = null
) = when(item) {
    is DashboardCategoriesViewItem -> {
        val horizontalPadding = LocalDimens.current.contentPadding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                )
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item.items.onEachIndexed { categoryIndex, categoryName ->
                val isSelected = item.selectedIndex == categoryIndex
                FilterChip(
                    onClick = {
                        onCategoryChanged?.invoke()
                        vm.onHeadlineCategoryChanged(categoryIndex)
                    },
                    selected = isSelected,
                ) {
                    Text(categoryName)
                }
            }
        }
    }
    is DashboardHeadlineViewItem -> {
        CustomAnnotatedTextListItem(
            modifier = modifier
                .alpha(if (item.isRead) {0.5f} else {1.0f})
                .clickable {
                    vm.onHeadlineClicked(item)
                }
                    ,
            trailing = {
                DownloadForOfflineButton(
                    modifier = Modifier.clickable {
                        vm.onAddHeadlineClicked(item)
                    }
                )
            },
            title = item.title,
            subtitle = item.description?.toAnnotatedString(
                linkColor = MaterialTheme.colors.secondary,
                onLinkClicked = { link ->
                    vm.onLinkClicked(link)
                }
            ),
        )
    }

    else -> {
        Text(
            text = "unknown item $item",
            modifier = modifier
        )
    }
}