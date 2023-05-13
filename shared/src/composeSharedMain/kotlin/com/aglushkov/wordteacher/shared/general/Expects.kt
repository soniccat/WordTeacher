package com.aglushkov.wordteacher.shared.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import dev.icerock.moko.resources.ImageResource

@Composable
expect fun painterFromImageResource(res: ImageResource): Painter
