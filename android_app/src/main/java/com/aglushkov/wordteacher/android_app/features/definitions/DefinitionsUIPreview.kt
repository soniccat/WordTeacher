package com.aglushkov.wordteacher.android_app.features.definitions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.aglushkov.wordteacher.android_app.compose.ComposeAppTheme
import com.aglushkov.wordteacher.shared.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.shared.features.definitions.views.DefinitionsVMPreview
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import java.io.IOException

@ExperimentalFoundationApi
@Preview
@Composable
private fun DefinitionsUIPreviewWithResponse() {
    ComposeAppTheme {
        DefinitionsUI(
            DefinitionsVMPreview(
                Resource.Loaded(
                    listOf(
                        DefinitionsDisplayModeViewItem(
                            partsOfSpeechFilterText = StringDesc.Raw("Noun"),
                            canClearPartsOfSpeechFilter = true,
                            modes = listOf(DefinitionsDisplayMode.BySource, DefinitionsDisplayMode.Merged),
                            selectedIndex = 0
                        ),
                        WordDividerViewItem(),
                        WordTitleViewItem(
                            title = "Word",
                            providers = listOf(Config.Type.Yandex)
                        ),
                        WordTitleViewItem(
                            title = "Word 2",
                            providers = listOf(Config.Type.Yandex, Config.Type.WordTeacher, Config.Type.Local)
                        ),
                        WordTranscriptionViewItem("[omg]"),
                        WordPartOfSpeechViewItem(StringDesc.Raw("Noun"), WordTeacherWord.PartOfSpeech.Noun),
                        WordDefinitionViewItem("* definition 1"),
                        WordDefinitionViewItem("* definition 2"),
                        WordSynonymViewItem("synonym 1", Indent.NONE),
                        WordSynonymViewItem("synonym 2", Indent.SMALL),
                        WordExampleViewItem("example 1", Indent.NONE),
                        WordExampleViewItem("example 2", Indent.SMALL),
                        WordSubHeaderViewItem(StringDesc.Raw("Subheader 1"), Indent.NONE),
                        WordSubHeaderViewItem(StringDesc.Raw("Subheader 2"), Indent.SMALL),
                    )
                )
            )
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun DefinitionsUIPreviewLoading() {
    ComposeAppTheme {
        DefinitionsUI(
            DefinitionsVMPreview(Resource.Loading())
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun DefinitionsUIPreviewError() {
    ComposeAppTheme {
        DefinitionsUI(
            DefinitionsVMPreview(
                Resource.Error(
                    IOException("Sth went wrong"),
                    true
                )
            )
        )
    }
}