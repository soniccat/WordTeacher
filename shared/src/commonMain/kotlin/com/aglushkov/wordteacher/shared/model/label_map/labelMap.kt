package com.aglushkov.wordteacher.shared.model.label_map

val labelToPartOfSpeechMap = buildMap {
    putAll(collinsCobuildLabelToPartOfSpeechMap)
    putAll(matthiasBuchmeierLabelToPartOfSpeechMap)
    putAll(ruLabelToPartOfSpeech)
}

val labelMap = buildMap {
    putAll(collinsCobuildLabelMap)
    putAll(matthiasBuchmeierLabelMap)
    putAll(ruLabelMap)
}