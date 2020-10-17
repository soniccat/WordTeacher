//
//  DefinitionsViewController.swift
//  iosApp
//
//  Created by Alexey Glushkov on 17.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import Cleanse
import shared

struct DefinitionsProperties {
    let backgroundColor: UIColor
    let vm: DefinitionsVM
}

class DefinitionsViewController: UIViewController {
    let props: DefinitionsProperties
    
    init(definitionsProperties: DefinitionsProperties) {
        self.props = definitionsProperties
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        self.view.backgroundColor = self.props.backgroundColor
        
        props.vm.definitions.addObserver { (res: Resource<NSArray>?) in
            let t = type(of: res)
            let isLoaded = res!.isLoaded() ? "true" : "false"
            print("status '\(t)' isLoaded: \(isLoaded)")
        }
    }
}

extension DefinitionsViewController {
    struct Module: Cleanse.Module {
        static func configure(binder: UnscopedBinder) {
            binder.bind().to { (manager: ConnectivityManager, wordRepository: WordRepository) in
                DefinitionsVM(connectivityManager: manager,
                              wordRepository: wordRepository,
                              state: DefinitionsVM.State(word: nil))
            }
            
            binder.bind(DefinitionsProperties.self).to { (vm: DefinitionsVM) -> DefinitionsProperties in
                DefinitionsProperties(backgroundColor: .blue, vm: vm)
            }
        }
    }
}
