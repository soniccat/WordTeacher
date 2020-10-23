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
        
        binder.bind().to {
            return ItemViewBinder()
                .addBlueprint(blueprint: DefinitionsDisplayModeBlueprint())
                .addBlueprint(blueprint: WordDefinitionBlueprint())
                .addBlueprint(blueprint: WordDividerBlueprint())
                .addBlueprint(blueprint: WordExampleBlueprint())
                .addBlueprint(blueprint: WordPartOfSpeechBlueprint())
                .addBlueprint(blueprint: WordSubHeaderBlueprint())
                .addBlueprint(blueprint: WordSynonymBlueprint())
                .addBlueprint(blueprint: WordTitleBlueprint())
                .addBlueprint(blueprint: WordTranscriptionBlueprint())
        }
    }
}
