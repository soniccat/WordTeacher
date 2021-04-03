package com.aglushkov.wordteacher.androidApp.features.definitions.views

import android.content.Context
import com.aglushkov.wordteacher.androidApp.general.views.chooser_dialog.ChooserDialog
import com.aglushkov.wordteacher.androidApp.general.views.chooser_dialog.ChooserViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.model.WordTeacherWord

fun showPartsOfSpeechFilterChooser(
    context: Context,
    idGenerator: IdGenerator,
    definitionsVM: DefinitionsVM,
    partsOfSpeech: List<WordTeacherWord.PartOfSpeech>,
    selectedPartsOfSpeech: List<WordTeacherWord.PartOfSpeech>
) {
    ChooserDialog(
        context
    ) { options ->
        definitionsVM.onPartOfSpeechFilterUpdated(
            options.filter { option ->
                option.isSelected
            }.map { option ->
                option.obj as WordTeacherWord.PartOfSpeech
            }
        )
    }.apply {
        show()
        showOptions(partsOfSpeech.map { partOfSpeech ->
            val isSelected = selectedPartsOfSpeech.contains(partOfSpeech)
            ChooserViewItem(idGenerator.nextId(), partOfSpeech.name, partOfSpeech, isSelected)
        })
    }
}