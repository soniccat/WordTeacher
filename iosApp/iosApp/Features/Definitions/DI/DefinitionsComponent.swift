//
//  DefinitionsComponent.swift
//  iosApp
//
//  Created by Alexey Glushkov on 17.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import Foundation
import Cleanse

struct DefinitionsComponent: Component {
    typealias Root = DefinitionsViewController
    
    static func configureRoot(binder bind: ReceiptBinder<DefinitionsViewController>) -> BindingReceipt<DefinitionsViewController> {
        bind.to(factory: DefinitionsViewController.init)
    }

    static func configure(binder: Binder<Unscoped>) {
        binder.include(module: DefinitionsViewController.Module.self)
    }
}
