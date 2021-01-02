//
//  DefinitionsComponent.swift
//  iosApp
//
//  Created by Alexey Glushkov on 17.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import Foundation
import Cleanse
import shared

public protocol DefinitionsDeps {
    var connectivityManager: ConnectivityManager { get }
    var wordRepository: WordRepository { get }
    var idGenerator: IdGenerator { get }
}

struct DefinitionsDepsModule: Module {
    typealias Scope = Unscoped
    
    static func configure(binder: Binder<Unscoped>) {
        binder.bind(ConnectivityManager.self).to { (seed: DefinitionsDeps) in
            return seed.connectivityManager
        }
        binder.bind(WordRepository.self).to { (seed: DefinitionsDeps) in
            return seed.wordRepository
        }
        binder.bind(IdGenerator.self).to { (seed: DefinitionsDeps) in
            return seed.idGenerator
        }
    }
}

public struct DefinitionsComponent: RootComponent {
    public typealias Root = DefinitionsViewController
    public typealias Seed = DefinitionsDeps

    public static func configureRoot(binder bind: ReceiptBinder<DefinitionsViewController>) -> BindingReceipt<DefinitionsViewController> {
        bind.to(factory: DefinitionsViewController.init)
    }

    public static func configure(binder: Binder<Singleton>) {
        binder.include(module: DefinitionsDepsModule.self)
        binder.include(module: DefinitionsModule.self)
    }
}
