package com.aglushkov.wordteacher.desktopapp.general.views.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun SearchView(
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
//            Icon(
//                painter = painterResource(15),
//                contentDescription = null,
//                tint = LocalContentColor.current
//            )
        },
        trailingIcon = {
            if (text.isNotEmpty()) {
//                Icon(
//                    painter = painterResource(R.drawable.ic_field_close_24),
//                    contentDescription = null,
//                    modifier = Modifier.clickable {
//                        onTextChanged("")
//                    },
//                    tint = LocalContentColor.current
//                )
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
