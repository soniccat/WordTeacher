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

public struct DefinitionsComponent: RootComponent {
    public typealias Root = DefinitionsViewController
    public typealias Seed = DefinitionsDeps

    public static func configureRoot(binder bind: ReceiptBinder<DefinitionsViewController>) -> BindingReceipt<DefinitionsViewController> {
        bind.to(factory: DefinitionsViewController.init)
    }

    public static func configure(binder: Binder<Unscoped>) {
        binder.include(module: DefinitionsModule.self)
    }
}
