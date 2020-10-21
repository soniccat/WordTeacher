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

class App: DefinitionsDeps {
    let wordRepository: WordRepository
    let connectivityManager: ConnectivityManager
    let idGenerator: IdGenerator
    
    init(
        wordRepository: WordRepository,
        connectivityManager: ConnectivityManager,
        idGenerator: IdGenerator
    ) {
        self.wordRepository = wordRepository
        self.connectivityManager = connectivityManager
        self.idGenerator = idGenerator
    }
}
