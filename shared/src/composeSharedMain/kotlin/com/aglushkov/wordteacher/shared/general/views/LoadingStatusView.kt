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
import com.aglushkov.wordteacher.shared.general.StringDescThrowable
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoadedAndEmpty
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.compose.localized
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc

@Composable
fun <T> LoadingStatusView(
    modifier: Modifier = Modifier,
    resource: Resource<T>,
    loadingText: String? = null,
    errorText: String? = null,
    tryAgainText: String? = null,
    emptyText: String?,
    tryAgainBlock: (() -> Unit)? = null
) where T : Collection<*> {
    if (resource.isLoadedAndEmpty() && emptyText != null) {
        LoadingStatusView(
            modifier,
            state = LoadingStatusViewState.Error(
                text = emptyText,
                tryAgainText = tryAgainText
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
fun <T> LoadingStatusViewWithErrorContent(
    modifier: Modifier = Modifier,
    resource: Resource<T>,
    loadingText: String? = null,
    errorText: String? = null,
    tryAgainText: String? = null,
    emptyText: String?,
    errorContent: @Composable (errorState: LoadingStatusViewState.Error) -> Unit,
) where T : Collection<*> {
    if (resource.isLoadedAndEmpty() && emptyText != null) {
        LoadingStatusViewWithErrorContent(
            modifier,
            state = LoadingStatusViewState.Error(
                text = emptyText,
                tryAgainText = tryAgainText
            ),
            errorContent
        )
    } else {
        LoadingStatusViewWithErrorContent(
            modifier,
            resource,
            loadingText,
            errorText,
            errorContent,
        )
    }
}

@Composable
fun <T> LoadingStatusViewWithErrorContent(
    modifier: Modifier = Modifier,
    resource: Resource<T>,
    loadingText: String? = null,
    errorText: String? = null,
    errorContent: @Composable (errorState: LoadingStatusViewState.Error) -> Unit,
) {
    val data = resource.data()
    val isEmptyList = data is List<*> && data.isEmpty()

    LoadingStatusViewWithErrorContent(
        modifier = modifier,
        state = resource.toLoadingStatusViewState(isEmptyList, loadingText, errorText),
        errorContent = errorContent
    )
}

@Composable
fun <T> LoadingStatusView(
    modifier: Modifier = Modifier,
    resource: Resource<T>,
    loadingText: String? = null,
    errorText: String? = StringDesc.Resource(MR.strings.error_default_loading_error).localized(),
    tryAgainBlock: (() -> Unit)? = null
) {
    val data = resource.data()
    val isEmptyList = data is List<*> && data.isEmpty()

    LoadingStatusView(
        modifier = modifier,
        state = resource.toLoadingStatusViewState(isEmptyList, loadingText, errorText),
        tryAgainBlock = tryAgainBlock
    )
}

@Composable
private fun <T> Resource<T>.toLoadingStatusViewState(
    isEmptyList: Boolean,
    loadingText: String?,
    errorText: String?
): LoadingStatusViewState = if (data() != null && !isEmptyList) {
    LoadingStatusViewState.Hidden
} else {
    when {
        isLoading() -> {
            LoadingStatusViewState.Loading(
                text = loadingText
            )
        }

        this is Resource.Error -> {
            val text = if (throwable is StringDescThrowable) {
                throwable.stringDesc.localized()
            } else {
                errorText
            }

            LoadingStatusViewState.Error(
                text = text,
                tryAgainText = stringResource(MR.strings.error_try_again)
            )
        }

        else -> {
            LoadingStatusViewState.Hidden
        }
    }
}

@Composable
fun LoadingStatusViewWithErrorContent(
    modifier: Modifier = Modifier,
    state: LoadingStatusViewState,
    errorContent: @Composable (errorState: LoadingStatusViewState.Error) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is LoadingStatusViewState.Loading -> {
                Column(
                    modifier = Modifier.padding(bottom = 50.dp),
                ) {
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
                errorContent?.invoke(state)
            }
            is LoadingStatusViewState.Hidden -> {
            }
        }
    }
}

@Composable
fun LoadingStatusView(
    modifier: Modifier = Modifier,
    state: LoadingStatusViewState,
    tryAgainBlock: (() -> Unit)? = null,
) = LoadingStatusViewWithErrorContent(
    modifier = modifier,
    state = state,
    errorContent = { errorState ->
        Column(
            modifier = Modifier.padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            errorState.text?.let { text ->
                Text(
                    text,
                    modifier = Modifier.padding(8.dp)
                )
            }
            errorState.tryAgainText?.let { text ->
                Button(
                    onClick = tryAgainBlock ?: {}
                ) {
                    Text(text = text)
                }
            }

        }
    }
)

sealed class LoadingStatusViewState {
    object Hidden: LoadingStatusViewState()
    data class Loading(val text: String?): LoadingStatusViewState()
    data class Error(
        val text: String?,
        val tryAgainText: String?
    ): LoadingStatusViewState()
}
