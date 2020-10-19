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

class DefinitionsViewController: UIViewController {
    @IBOutlet var collectionView: UICollectionView!
    
    var adapter: SimpleAdapter!
    let vm: DefinitionsVM
    
    init(vm: DefinitionsVM) {
        self.vm = vm
        super.init(nibName: "DefinitionsViewController", bundle: Bundle.main)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        bindView()
        observeViewModel()
    }
    
    private func observeViewModel() {
        vm.definitions.addObserver { [weak self] (res: Resource<NSArray>?) in
            self?.showDefinitions(res: res!)
        }
    }
    
    private func bindView() {
        adapter = SimpleAdapter(collectionView: collectionView)
    }
    
    private func showDefinitions(res: Resource<NSArray>) {
        if res.isLoaded() {
            if let items = res.data() {
                var snapshot = adapter.dataSource.snapshot()
                snapshot.appendSections([.main])
                snapshot.appendItems([items.firstObject] as! [BaseViewItem<AnyObject>])
                
                adapter.dataSource.apply(snapshot, animatingDifferences: true)
            }
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
        }
    }
}
