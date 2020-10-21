//
//  AppComponent.swift
//  iosApp
//
//  Created by Alexey Glushkov on 17.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import Cleanse
import shared

struct AppComponent : Cleanse.RootComponent {
    typealias Root = App

    static func configureRoot(binder bind: ReceiptBinder<App>) -> BindingReceipt<App> {
        bind.to(factory: App.init)
    }

    static func configure(binder: SingletonBinder) {
        //binder.install(dependency: DefinitionsComponent.self)
        binder.include(module: AppModule.self)
    }
}
