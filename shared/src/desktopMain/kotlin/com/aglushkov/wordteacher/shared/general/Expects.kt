package com.aglushkov.wordteacher.shared.general

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.aglushkov.wordteacher.shared.general.views.AddIcon
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.ImageResource

@Composable
actual fun painterFromImageResource(res: ImageResource): Painter {
    return painterResource(MR.images.plus_small.filePath)
}