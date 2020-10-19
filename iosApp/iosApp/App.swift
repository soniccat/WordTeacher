//
//  App.swift
//  iosApp
//
//  Created by Alexey Glushkov on 17.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import Foundation
import Cleanse
import shared

class App {
    private let definitionsComponentFactory: ComponentFactory<DefinitionsComponent>
    let wordRepository: WordRepository
    let connectivityManager: ConnectivityManager
    
    init(
        definitionsComponentFactory: ComponentFactory<DefinitionsComponent>,
        wordRepository: WordRepository,
        connectivityManager: ConnectivityManager
    ) {
        self.definitionsComponentFactory = definitionsComponentFactory
        self.wordRepository = wordRepository
        self.connectivityManager = connectivityManager
    }
    
    func definitionsVC() -> DefinitionsViewController {
        return definitionsComponentFactory.build(())
    }
}
