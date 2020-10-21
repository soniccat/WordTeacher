//
//  SimpleAdapter.swift
//  iosApp
//
//  Created by Alexey Glushkov on 18.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

protocol Blueprint {
    associatedtype T: AnyObject
    associatedtype V: UIView
    associatedtype I: BaseViewItem<T>
    
    var type: Int { get }
    var cellIdentifier: String { get }
    var nibName: String { get }

    func bind(view: V, viewItem: I)
}

extension Blueprint {
    var nib: UINib {
        get {
            return UINib(nibName: nibName, bundle: nil)
        }
    }
}

class ItemViewBinder {
    var blueprints = Dictionary<Int, AnyObject>()

    func addBlueprint(blueprint: AnyObject) -> ItemViewBinder {
        let castedBlueprint = blueprint as! Blueprint
        blueprints[castedBlueprint.type] = blueprint
        return self
    }

    func findBlueprint(type: Int) -> AnyObject? {
        return blueprints[type]
    }
}


enum Section {
  case main
}

class SimpleAdapter {
    private var binder: ItemViewBinder
    var dataSource: UICollectionViewDiffableDataSource<Section, BaseViewItem<AnyObject>>
    
    init(binder: ItemViewBinder, collectionView: UICollectionView) {
        self.binder = binder
        self.binder.blueprints.forEach { (key: Int, value: Blueprint<AnyObject, BaseViewItem<AnyObject>, AnyObject>) in
            collectionView.register(value.nib, forCellWithReuseIdentifier: value.cellIdentifier)
        }
        
        collectionView.register(UINib(nibName: "TextCell", bundle: Bundle.main), forCellWithReuseIdentifier: "TextCell")
        
        dataSource = UICollectionViewDiffableDataSource<Section, BaseViewItem<AnyObject>>(
        collectionView: collectionView) { (collectionView, indexPath, item) -> UICollectionViewCell? in
            let blueprint = binder.findBlueprint(type: Int(item.type))!
            let cell = collectionView.dequeueReusableCell(
                withReuseIdentifier: blueprint.cellIdentifier,
                for: indexPath)
            blueprint.bind(view: cell, viewItem: item)
            //cell?.textView.text = item.firstItem() as? String ?? "class " + type(of: item).description()
            return cell
        }
    }
}
