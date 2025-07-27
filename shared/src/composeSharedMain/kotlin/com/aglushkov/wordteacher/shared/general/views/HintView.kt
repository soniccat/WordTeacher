package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.settings.HintInlineContent
import com.aglushkov.wordteacher.shared.general.settings.HintType
import com.aglushkov.wordteacher.shared.general.settings.toAttributedString
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.painterResource

@Composable
fun HintView(
    modifier: Modifier,
    hintType: HintType,
    contentPadding: PaddingValues = PaddingValues(horizontal = LocalDimens.current.contentPadding)
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(contentPadding)
            .background(
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.10f),
                shape = RoundedCornerShape(6.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicText(
                text = hintType.toAttributedString(),
                modifier = Modifier.weight(1.0f)
                    .padding(
                        start = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
                style = LocalAppTypography.current.hintStyle,
                inlineContent = HintInlineContent,
            )
            Icon(
                painter = painterResource(MR.images.close_18),
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .then(modifier)
                    .padding(4.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.87f)
            )
        }
    }
}
