package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.painterResource

@Composable
fun DownloadForOfflineButton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        LocalDimens.current.halfOfContentPadding
    )
) {
    Icon(
        painterResource(MR.images.download_for_offline),
        null,
        modifier = Modifier.clip(CircleShape).then(modifier).padding(contentPadding),
        tint = MaterialTheme.colors.secondary
    )
}
