@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class, ExperimentalMaterialApi::class
)

package com.aglushkov.wordteacher.shared.features.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsStartWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.contentColorFor
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.features.articles.views.ArticleTitleView
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.views.CardSetItemView
import com.aglushkov.wordteacher.shared.features.cardsets.views.CardSetSearchItemView
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.RemoteCardSetViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardCategoriesViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardExpandViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardHeadlineViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardTryAgainViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardVM
import com.aglushkov.wordteacher.shared.features.dashboard.vm.HintViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordLoadingViewItem
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsViewTitleItem
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

@Composable
fun DashboardUI(
    vm: DashboardVM,
    modifier: Modifier = Modifier,
) {
    val itemsState by vm.viewItems.collectAsState()
    val items = itemsState.data()

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column {
            TopAppBar(
                title = { Text(stringResource(MR.strings.dashboard_title)) },
            )

            if (itemsState.isLoaded() || items != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().windowInsetsHorizontalPadding(),
                    contentPadding = PaddingValues(
                        bottom = 100.dp
                    )
                ) {
                    items(items.orEmpty(), key = { it.id }) { item ->
                        dashboardItem(
                            Modifier.animateItem(),
                            item,
                            vm
                        )
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
fun dashboardItem(
    modifier: Modifier,
    item: BaseViewItem<*>,
    vm: DashboardVM,
) = when(item) {
    is CardSetViewItem -> CardSetItemView(
        Modifier.clickable {
            vm.onCardSetClicked(item)
        },
        item,
        trailing = {
            val side = 40.dp
            Box(
                modifier = Modifier.size(side, side)
            ) {
                CircularProgressIndicator(
                    progress = 1.0f,
                    modifier = Modifier.padding(5.dp),
                    color = Color.LightGray.copy(alpha = 0.2f)
                )
                CircularProgressIndicator(
                    progress = item.totalProgress,
                    modifier = Modifier.padding(5.dp),
                )
                StartLearningButton(
                    modifier = Modifier.clickable {
                        vm.onCardSetStartLearningClicked(item)
                    }
                )
            }
        }
    )
    is ArticleViewItem -> ArticleTitleView(
        Modifier.clickable {
            vm.onArticleClicked(item)
        },
        item,
    )
    is DashboardCategoriesViewItem -> {
        val horizontalPadding = LocalDimens.current.contentPadding
        Row(
            modifier = Modifier
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
                    onClick = { vm.onHeadlineCategoryChanged(categoryIndex) },
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
    is DashboardExpandViewItem -> {
        Button(
            onClick = {
                vm.onExpandClicked(item)
            },
            modifier = Modifier.padding(
                horizontal = LocalDimens.current.contentPadding
            ).heightIn(min = 28.dp),
            contentPadding = PaddingValues(
                start = 8.dp,
                top = 4.dp,
                end = 8.dp,
                bottom = 4.dp
            ),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primarySurface
            )
        ) {
            Text(
                if (item.isExpanded) {
                    ResourceStringDesc(MR.strings.default_collapse).localized()
                } else {
                    ResourceStringDesc(MR.strings.default_expand).localized()
                },
                color = contentColorFor(MaterialTheme.colors.primarySurface)
            )
        }

    }
    is DashboardTryAgainViewItem -> {
        Column(
            modifier = Modifier.padding(LocalDimens.current.contentPadding).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(item.errorText.localized(), style = LocalAppTypography.current.settingsText)
            Button(
                onClick = { vm.onDashboardTryAgainClicked() },
            ) {
                Text(text = item.tryAgainActionText.localized())
            }
        }
    }
    is RemoteCardSetViewItem -> {
        CardSetSearchItemView(
            item,
            onClick = { vm.onRemoteCardSetClicked(item) },
        )
    }
    is SettingsViewTitleItem -> {
        ListItem (
            modifier = Modifier.padding(top = 8.dp),
            text = {
                Text(item.firstItem().localized(),
                style = LocalAppTypography.current.settingsTitle)
            }
        )
    }
    is WordLoadingViewItem -> {
        Box(Modifier.fillMaxWidth().padding(LocalDimens.current.contentPadding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
    is HintViewItem -> {
        HintView(
            hintType = item.firstItem(),
            contentPadding = PaddingValues(
                start = LocalDimens.current.contentPadding,
                end = LocalDimens.current.contentPadding,
                top = if (item.firstItem() == HintType.Introduction)
                    LocalDimens.current.contentPadding
                else
                    0.dp,
            ),
            onHidden = { vm.onHintClicked(item.firstItem()) }
        )
    }
    else -> {
        Text(
            text = "unknown item $item",
            modifier = modifier
        )
    }
}