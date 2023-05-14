package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoadedAndEmpty
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun <T> LoadingStatusView(
    modifier: Modifier = Modifier,
    resource: Resource<T>,
    loadingText: String? = null,
    errorText: String? = null,
    emptyText: String?,
    tryAgainBlock: (() -> Unit)? = null
) where T : Collection<*> {
    if (resource.isLoadedAndEmpty() && emptyText != null) {
        LoadingStatusView(
            modifier,
            state = LoadingStatusViewState.Error(
                text = emptyText,
                tryAgainText = null
            ),
            tryAgainBlock
        )
    } else {
        LoadingStatusView(
            modifier,
            resource,
            loadingText,
            errorText,
            tryAgainBlock
        )
    }
}

@Composable
fun <T> LoadingStatusView(
    modifier: Modifier = Modifier,
    resource: Resource<T>,
    loadingText: String? = null,
    errorText: String? = null,
    tryAgainBlock: (() -> Unit)? = null
) {
    val data = resource.data()
    val isEmptyList = data is List<*> && data.isEmpty()

    LoadingStatusView(
        modifier = modifier,
        state = if (data != null && !isEmptyList) {
            LoadingStatusViewState.Hidden
        } else {
            when {
                resource.isLoading() -> {
                    LoadingStatusViewState.Loading(
                        text = loadingText
                    )
                }
                resource is Resource.Error -> {
                    LoadingStatusViewState.Error(
                        text = errorText,
                        tryAgainText = stringResource(MR.strings.error_try_again)
                    )
                }
                else -> {
                    LoadingStatusViewState.Hidden
                }
            }
        },
        tryAgainBlock = tryAgainBlock
    )
}

@Composable
fun LoadingStatusView(
    modifier: Modifier = Modifier,
    state: LoadingStatusViewState,
    tryAgainBlock: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is LoadingStatusViewState.Loading -> {
                Column {
                    CircularProgressIndicator()
                    state.text?.let { text ->
                        Text(
                            text,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
            is LoadingStatusViewState.Error -> {
                Column {
                    state.text?.let { text ->
                        Text(
                            text,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    state.tryAgainText?.let { text ->
                        Button(
                            onClick = tryAgainBlock ?: {}
                        ) {
                            Text(text = text)
                        }
                    }
                }
            }
            is LoadingStatusViewState.Hidden -> {
            }
        }
    }
}

sealed class LoadingStatusViewState {
    object Hidden: LoadingStatusViewState()
    data class Loading(val text: String?): LoadingStatusViewState()
    data class Error(
        val text: String?,
        val tryAgainText: String?
    ): LoadingStatusViewState()
}
