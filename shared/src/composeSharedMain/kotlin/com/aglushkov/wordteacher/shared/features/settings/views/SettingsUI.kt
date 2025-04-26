package com.aglushkov.wordteacher.shared.features.settings.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.features.settings.vm.*
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.views.CustomListItem
import com.aglushkov.wordteacher.shared.general.views.LoadingViewItemUI
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.compose.localized

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsUI(
    vm: SettingsVM,
    modifier: Modifier = Modifier,
) {
    val itemsState = vm.items.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column {
            TopAppBar(
                title = { Text(stringResource(MR.strings.settings_title)) },
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    bottom = 100.dp
                )
            ) {
                items(itemsState.value, key = { it.id }) { item ->
                    showSettingsItem(
                        Modifier.animateItemPlacement(),
                        item,
                        vm
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun showSettingsItem(
    modifier: Modifier,
    item: BaseViewItem<*>,
    vm: SettingsVM,
) = when (item) {
    is SettingsViewTitleItem -> {
        ListItem (
            text = { Text(item.firstItem().localized(), style = LocalAppTypography.current.settingsTitle) }
        )
    }
    is SettingsViewTextItem -> {
        CustomListItem(
            contentPadding = PaddingValues(
                start = LocalDimens.current.contentPadding,
                end = LocalDimens.current.contentPadding,
                bottom = if (item.withBottomPadding) {
                    LocalDimens.current.contentPadding
                } else {
                    0.dp
                }
            ),
            content = { Text(item.firstItem().localized(), style = LocalAppTypography.current.settingsText) }
        )
    }
    is SettingsViewLoading -> {
        LoadingViewItemUI()
    }
    is SettingsSignInItem -> {
        Row(
            modifier = Modifier.padding(
                start = LocalDimens.current.contentPadding - 8.dp
            )
        ) {
            item.networkTypes.onEach { networkType ->
                Image(
                    painter = painterResource(
                        when (networkType) {
                            SpaceAuthService.NetworkType.Google -> MR.images.google
                            SpaceAuthService.NetworkType.VKID -> MR.images.vk
                            SpaceAuthService.NetworkType.YandexId -> MR.images.yandex
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(90.dp)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .clickable {
                            vm.onSignInClicked(networkType)
                        }
                )
            }
        }
    }
    is SettingsSignOutItem -> {
        Button(
            onClick = { vm.onSignOutClicked() },
            modifier = Modifier.padding(start = LocalDimens.current.contentPadding)
        ) {
            Text(text = item.firstItem().localized())
        }
    }
    is SettingsViewAuthRefreshButtonItem -> {
        Button(
            onClick = { vm.onAuthRefreshClicked() },
            modifier = Modifier.padding(start = LocalDimens.current.contentPadding, bottom = LocalDimens.current.contentPadding)
        ) {
            Text(text = item.firstItem().localized())
        }
    }
    is SettingsOpenDictConfigsItem -> {
        CustomListItem(
            modifier = Modifier
                .clickable {
                    vm.router?.openDictConfigs()
                },
            trailing = {
                Icon(
                    painter = painterResource(MR.images.arrow_right_24),
                    contentDescription = null,
                    tint = LocalContentColor.current
                )
            },
        ){
            Text(
                stringResource(MR.strings.settings_dictconfigs),
                style = LocalAppTypography.current.settingsTitle
            )
        }
    }
    is SettingsWordFrequencyUploadFileItem -> {
        Button(
            onClick = { vm.onUploadWordFrequencyFileClicked() },
            modifier = Modifier.padding(start = LocalDimens.current.contentPadding)
        ) {
            Text(text = item.firstItem().localized())
        }
    }
    is SettingsLogsConfigsItem -> {
        CustomListItem(
            modifier = Modifier
                .clickable {
                    vm.onLoggingIsEnabledChanged()
                },
            contentPadding = PaddingValues(
                start = LocalDimens.current.contentPadding,
                end = LocalDimens.current.contentPadding,
                bottom = LocalDimens.current.contentPadding
            ),
            trailing = {
                Checkbox(
                    checked = item.isEnabled,
                    onCheckedChange = null
                )
            },
            content = { Text(stringResource(MR.strings.settings_logging_enabled)) },
        )

        if (item.paths.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                var expanded by remember { mutableStateOf(false) }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    item.paths.map { logFileItem ->
                        DropdownMenuItem(
                            onClick = {
                                vm.onLogFileShareClicked(logFileItem.path)
                                expanded = false
                            }
                        ) {
                            Text(logFileItem.path.name)
                        }
                    }
                }
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.padding(
                        start = LocalDimens.current.contentPadding,
                    )
                ) {
                    Text(stringResource(MR.strings.settings_logging_share))
                }
            }
        } else {
            Unit
        }
    }
    is SettingsPrivacyPolicyItem -> {
        CustomListItem(
            modifier = Modifier
                .clickable {
                    vm.onPrivacyPolicyClicked()
                },
            contentPadding = PaddingValues(
                start = LocalDimens.current.contentPadding,
                end = LocalDimens.current.contentPadding,
                bottom = LocalDimens.current.contentPadding
            ),
            content = {
                Text(stringResource(MR.strings.settings_open_privacy_policy))
            },
        )
    }
    is SettingsAbout -> {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clickable { vm.onEmailClicked() }
                .padding(LocalDimens.current.contentPadding)
        ) {
            Text(item.appTitle)
            Text(item.email)
        }
    }
    else -> {
        Text(
            text = "unknown item $item",
            modifier = modifier
        )
    }
}
