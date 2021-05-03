package com.aglushkov.wordteacher.androidApp.features.definitions.views

import android.widget.EditText
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM

@Composable
fun DefinitionsUI(vm: DefinitionsVM) {
    val defs = vm.definitions.collectAsState()
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CustomTopAppBar(
            ) {
                SearchView(searchText, { searchText = it }) {
                    vm.onWordSubmitted(searchText)
                }
            }
        },
        bottomBar = {

        }
    ) {
        Text("hi " + defs.value.data().orEmpty().size)
    }

//    Button(onClick = {
//        component.onWordSubmitted("owl")
//    }) {
//        Text("hi " + defs.value.data().orEmpty().size)
//    }
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
    backgroundColor: Color = MaterialTheme.colors.primarySurface,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
    contentPadding: PaddingValues = AppBarDefaults.ContentPadding,
    shape: Shape = RectangleShape,
    modifier: Modifier = Modifier,
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
