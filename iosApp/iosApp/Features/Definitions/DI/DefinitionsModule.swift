//
//  DefinitionsModule.swift
//  iosApp
//
//  Created by Alexey Glushkov on 21.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import Foundation
import Cleanse
import shared

struct DefinitionsModule: Cleanse.Module {
    struct AssistedSeed : AssistedFactory {
      typealias Seed = DefinitionsDeps
      typealias Element = DefinitionsVM
    }
    
    static func configure(binder: UnscopedBinder) {
//        binder.bind().to {
//            DefinitionsVM(connectivityManager: $0,
//                          wordRepository: $1,
//                          idGenerator: $2,
//                          state: DefinitionsVM.State(word: nil))
//        }
        
//        binder.binder
    }
}
