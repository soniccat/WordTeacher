@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class, ExperimentalMaterialApi::class
)

package com.aglushkov.wordteacher.shared.features.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.features.cardsets.views.CardSetSearchItemView
import com.aglushkov.wordteacher.shared.features.cardsets.vm.RemoteCardSetViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardCategoriesViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardHeadlineViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardTryAgainViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordLoadingViewItem
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsVM
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsViewTitleItem
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.hasData
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.compose.localized

@Composable
fun DashboardUI(
    vm: DashboardVM,
    modifier: Modifier = Modifier
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
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        bottom = 100.dp
                    )
                ) {
                    items(items.orEmpty(), key = { it.id }) { item ->
                        dashboardItem(
                            Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
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
    is DashboardCategoriesViewItem -> {
        val horizontalPadding = LocalDimens.current.definitionsDisplayModeHorizontalPadding
        Row(
            modifier = Modifier
                .then(modifier)
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                )
                .horizontalScroll(rememberScrollState())
        ) {
            item.items.onEachIndexed { categoryIndex, categoryName ->
                val isSelected = item.selectedIndex == categoryIndex
                FilterChip(
                    onClick = { vm.onHeadlineCategoryChanged(categoryIndex) },
                    selected = isSelected,
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = "Done icon",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        null
                    },
                ) {
                    Text(categoryName)
                }
            }

        }
    }
    is DashboardHeadlineViewItem -> {
        ListItem(
            modifier = Modifier
                .then(modifier),
            secondaryText = {
                item.description?.let {
                    Text(text = it, maxLines = 2)
                }
            }
        ) {
            Text(text = item.title)
        }
    }
    is DashboardTryAgainViewItem -> {
        TODO("DashboardTryAgainViewItem")
    }
    is RemoteCardSetViewItem -> {
        CardSetSearchItemView(
            item,
            onClick = { vm.onCardSetClicked(item) },
        )
    }
    is SettingsViewTitleItem -> {
        ListItem (
            text = { Text(item.firstItem().localized(), style = LocalAppTypography.current.settingsTitle) }
        )
    }
    is WordLoadingViewItem -> {
        Box(Modifier.fillMaxWidth().padding(LocalDimens.current.contentPadding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
    else -> {
        Text(
            text = "unknown item $item",
            modifier = modifier
        )
    }
}