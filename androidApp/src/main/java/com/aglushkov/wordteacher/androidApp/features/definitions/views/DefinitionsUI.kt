package com.aglushkov.wordteacher.androidApp.features.definitions.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.contentColorFor
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import dev.icerock.moko.resources.desc.StringDesc

@Composable
fun DefinitionsUI(vm: DefinitionsVM) {
    val defs = vm.definitions.collectAsState()
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CustomTopAppBar {
                SearchView(searchText, { searchText = it }) {
                    vm.onWordSubmitted(searchText)
                }
            }
        },
        bottomBar = {
        }
    ) {
        val res = defs.value
        val data = res.data()

        if (data?.isNotEmpty() == true) {
            LazyColumn {
                items(data) { item ->
                    when (item) {
                        is DefinitionsDisplayModeViewItem -> {
                            val horizontalPadding = dimensionResource(R.dimen.definitions_displayMode_horizontal_padding)
                            val topPadding = dimensionResource(R.dimen.definitions_displayMode_vertical_padding)
                            Row(
                                modifier = Modifier.padding(
                                    start = horizontalPadding,
                                    end = horizontalPadding,
                                    top = topPadding
                                )
                            ) {
                                Chip(
                                    text = item.partsOfSpeechFilterText.resolveString(),
                                    colors = ChipColors(
                                        contentColor = MaterialTheme.colors.onSecondary,
                                        bgColor = MaterialTheme.colors.secondary
                                    ),
                                    isCloseIconVisible = item.canClearPartsOfSpeechFilter
                                ) {
                                    vm.onPartOfSpeechFilterClicked(item)
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = "unknown item $item"
                            )
                        }
                    }
                }
            }
        } else {
            LoadingStatusView(
                resource = res,
                loadingText = null,
                errorText = vm.getErrorText(res)?.resolveString(),
                emptyText = LocalContext.current.getString(R.string.error_no_definitions)
            ) {
                vm.onTryAgainClicked()
            }
        }
    }
}

@Composable
private fun SearchView(
    text: String,
    onTextChanged: (String) -> Unit,
    onImeAction: () -> Unit
) {
    TextField(
        value = text,
        onValueChange = onTextChanged,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(2.dp)
            ),
        textStyle = LocalTextStyle.current.copy(
            color = MaterialTheme.colors.onSurface
        ),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_field_search_24),
                contentDescription = null,
                tint = LocalContentColor.current
            )
        },
        trailingIcon = {
            if (text.isNotEmpty()) {
                Icon(
                    painter = painterResource(R.drawable.ic_field_close_24),
                    contentDescription = null,
                    modifier = Modifier.clickable {
                        onTextChanged("")
                    },
                    tint = LocalContentColor.current
                )
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onImeAction()
            }
        ),
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
// custom top app bar to get wrap content height
private fun CustomTopAppBar(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.primarySurface,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
    contentPadding: PaddingValues = AppBarDefaults.ContentPadding,
    shape: Shape = RectangleShape,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
        shape = shape,
        modifier = modifier
    ) {
        Row(
            Modifier.fillMaxWidth().padding(contentPadding),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun Chip(
    modifier: Modifier = Modifier,
    text: String,
    isChecked: Boolean = false,
    colors: ChipColors? = null,
    isCloseIconVisible: Boolean = false,
    closeBlock: (() -> Unit)? = null,
    clickBlock: (() -> Unit)? = null
) {
    Surface(
        color = when {
            isChecked -> colors?.checkedBgColor ?: MaterialTheme.colors.onSurface.copy(alpha = 0.18f)
            else -> colors?.bgColor ?: MaterialTheme.colors.onSurface.copy(alpha = 0.10f)
        },
        contentColor = colors?.contentColor ?: MaterialTheme.colors.onSurface,
//        contentColor = when {
//            isSelected -> MaterialTheme.colors.primary
//            else -> MaterialTheme.colors.onSurface
//        },
        shape = CircleShape,
        modifier = modifier.clickable {
            clickBlock?.invoke()
        }
    ) {
        Row {
            if (isChecked) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_24),
                    contentDescription = null
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (isCloseIconVisible && closeBlock != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_18),
                    contentDescription = null,
                    modifier = Modifier.clickable {
                        closeBlock()
                    },
                    tint = colors?.closeIconTint ?: MaterialTheme.colors.onSurface.copy(alpha = 0.87f)
                )
            }
        }
    }
}

data class ChipColors(
    val contentColor: Color? = null,
    val bgColor: Color? = null,
    val checkedBgColor: Color? = bgColor,
    val closeIconTint: Color? = contentColor
)

@Composable
fun StringDesc.resolveString() = toString(LocalContext.current)