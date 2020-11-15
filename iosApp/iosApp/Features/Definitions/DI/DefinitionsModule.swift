//
//  DefinitionsModule.swift
//  iosApp
//
//  Created by Alexey Glushkov on 22.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import Foundation
import Cleanse
import shared

class DefinitionsModule: Module {
    typealias Scope = Unscoped
    
    static func configure(binder: Binder<Unscoped>) {
        binder.bind().to { DefinitionsVM.State(word: nil) }
        binder.bind().to(factory: DefinitionsVM.init.self)
        binder.bind().to(factory: DefinitionsDisplayModeBlueprint.init.self)
        binder.bind().to(factory: WordDefinitionBlueprint.init.self)
        binder.bind().to(factory: WordDividerBlueprint.init.self)
        binder.bind().to(factory: WordExampleBlueprint.init.self)
        binder.bind().to(factory: WordPartOfSpeechBlueprint.init.self)
        binder.bind().to(factory: WordSubHeaderBlueprint.init.self)
        binder.bind().to(factory: WordSynonymBlueprint.init.self)
        binder.bind().to(factory: WordTitleBlueprint.init.self)
        binder.bind().to(factory: WordTranscriptionBlueprint.init.self)
        
        binder.bind().to { (
                definitionsDisplayModeBlueprint: DefinitionsDisplayModeBlueprint,
                wordDefinitionBlueprint: WordDefinitionBlueprint,
                wordDividerBlueprint: WordDividerBlueprint,
                wordExampleBlueprint: WordExampleBlueprint,
                wordPartOfSpeechBlueprint: WordPartOfSpeechBlueprint,
                wordSubHeaderBlueprint: WordSubHeaderBlueprint,
                wordSynonymBlueprint: WordSynonymBlueprint,
                wordTitleBlueprint: WordTitleBlueprint,
                wordTranscriptionBlueprint: WordTranscriptionBlueprint
            ) in
            return ItemViewBinder()
                .addBlueprint(blueprint: definitionsDisplayModeBlueprint)
                .addBlueprint(blueprint: wordDefinitionBlueprint)
                .addBlueprint(blueprint: wordDividerBlueprint)
                .addBlueprint(blueprint: wordExampleBlueprint)
                .addBlueprint(blueprint: wordPartOfSpeechBlueprint)
                .addBlueprint(blueprint: wordSubHeaderBlueprint)
                .addBlueprint(blueprint: wordSynonymBlueprint)
                .addBlueprint(blueprint: wordTitleBlueprint)
                .addBlueprint(blueprint: wordTranscriptionBlueprint)
        }
    }
}
