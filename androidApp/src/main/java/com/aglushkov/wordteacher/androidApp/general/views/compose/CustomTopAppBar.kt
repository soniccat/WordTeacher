package com.aglushkov.wordteacher.androidApp.general.views.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

@Composable
// custom top app bar to get wrap content height
fun CustomTopAppBar(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.primarySurface,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
    contentPadding: PaddingValues = AppBarDefaults.ContentPadding,
    shape: Shape = RectangleShape,
    content: @Composable RowScope.() -> Unit
) {
    // HACK: after switchig to 1.2.0-beta02 compose using Surface with primarySurface color
    // changes it globally and breaks colors of other compositions... need to check in the next version
    // for now there is my simple surface
    Box(
        modifier = modifier
            .mySurface(
                shape = shape,
                backgroundColor = backgroundColor,
                border = null,
                elevation = elevation
            )
            .semantics(mergeDescendants = false) {}
            .pointerInput(Unit) {},
        propagateMinConstraints = true
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

fun Modifier.mySurface(
    shape: Shape,
    backgroundColor: Color,
    border: BorderStroke?,
    elevation: Dp
) = this.shadow(elevation, shape, clip = false)
    .then(if (border != null) Modifier.border(border, shape) else Modifier)
    .background(color = backgroundColor, shape = shape)
    .clip(shape)