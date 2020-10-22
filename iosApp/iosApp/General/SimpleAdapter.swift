//
//  SimpleAdapter.swift
//  iosApp
//
//  Created by Alexey Glushkov on 18.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

enum Section {
  case main
}

class SimpleAdapter {
    private var binder: ItemViewBinder
    var dataSource: UICollectionViewDiffableDataSource<Section, BaseViewItem<AnyObject>>
    
    init(binder: ItemViewBinder, collectionView: UICollectionView) {
        self.binder = binder
        self.binder.blueprints.forEach { entity in
            let (_, value) = entity
            value.register(collectionView: collectionView)
        }
        
        dataSource = UICollectionViewDiffableDataSource<Section, BaseViewItem<AnyObject>>(
        collectionView: collectionView) { (collectionView, indexPath, item) -> UICollectionViewCell? in
            let blueprint = binder.findBlueprint(type: Int(item.type))!
            let cell = collectionView.dequeueReusableCell(
                withReuseIdentifier: blueprint.cellIdentifier,
                for: indexPath)
            blueprint.bind(view: cell, viewItem: item)
            return cell
        }
    }
}
