//
//  AppModule.swift
//  iosApp
//
//  Created by Alexey Glushkov on 17.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import Foundation
import Cleanse
import shared

struct AppModule : Module {
    static func configure(binder: SingletonBinder) {
        binder.bind().sharedInScope().to { ConfigService(baseUrl: "https://soniccat.ru/") }
        binder.bind().sharedInScope().to(factory: ConnectivityManager.init)
        binder.bind().sharedInScope().to(factory: ConfigRepository.init)
        binder.bind().sharedInScope().to(factory: ConfigConnectParamsStatFile.init)
        binder.bind().sharedInScope().to(factory: ConfigConnectParamsStatRepository.init)
        binder.bind().sharedInScope().to(factory: WordTeacherWordServiceFactory.init)
        binder.bind().sharedInScope().to(factory: ServiceRepository.init)
        binder.bind().sharedInScope().to(factory: WordRepository.init)
    }
}
