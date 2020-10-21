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

public class DefinitionsViewController: UIViewController, UICollectionViewDelegateFlowLayout {
    @IBOutlet var collectionView: UICollectionView!
    
    var adapter: SimpleAdapter!
    let vm: DefinitionsVM
    
    init(deps: DefinitionsDeps) {
        let vm = DefinitionsVM(
            connectivityManager: deps.connectivityManager,
            wordRepository: deps.wordRepository,
            idGenerator: deps.idGenerator,
            state: DefinitionsVM.State(word: nil)
        )

        self.vm = vm
        super.init(nibName: "DefinitionsViewController", bundle: Bundle.main)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override func viewDidLoad() {
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
        let flowLayout = collectionView.collectionViewLayout as! UICollectionViewFlowLayout
        flowLayout.estimatedItemSize = UICollectionViewFlowLayout.automaticSize
        adapter = SimpleAdapter(collectionView: collectionView)
    }
    
    private func showDefinitions(res: Resource<NSArray>) {
        if res.isLoaded() {
            if let items = res.data() {
                var snapshot = adapter.dataSource.snapshot()
                snapshot.appendSections([.main])
                snapshot.appendItems(items as! [BaseViewItem<AnyObject>])
                
                adapter.dataSource.apply(snapshot, animatingDifferences: true)
            }
        }
    }
    
    public func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        // to get the right width in preferredLayoutAttributesFitting
        return CGSize(width: collectionView.bounds.width, height: 1000)
    }
}
