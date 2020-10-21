//
//  SimpleAdapter.swift
//  iosApp
//
//  Created by Alexey Glushkov on 18.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

// Type erased Blueprint
protocol BaseBlueprint {
    var type: Int { get }
    var cellIdentifier: String { get }
    var nibName: String { get }
    var nib: UINib { get }

    func bind(view: UIView, viewItem: AnyObject)
}

extension BaseBlueprint {
    var nib: UINib {
        get {
            return UINib(nibName: nibName, bundle: nil)
        }
    }
}

protocol Blueprint: BaseBlueprint {
    associatedtype T: AnyObject
    associatedtype V: UIView
    
    var type: Int { get }
    var cellIdentifier: String { get }
    var nibName: String { get }

    func bind(view: V, viewItem: T)
}

class AnyBlueprint {
    private var blueprint: BaseBlueprint
    
    var type: Int { get { blueprint.type } }
    var cellIdentifier: String { get { blueprint.cellIdentifier } }
    var nibName: String { get { blueprint.nibName } }
    var nib: UINib { get { blueprint.nib } }
    
    init(blueprint: BaseBlueprint) {
        self.blueprint = blueprint
    }
    
    func bind(view: UIView, viewItem: AnyObject) {
        self.blueprint.bind(view: view, viewItem: viewItem)
    }
}

class ItemViewBinder {
    var blueprints = Dictionary<Int, AnyBlueprint>()

    func addBlueprint(blueprint: BaseBlueprint) -> ItemViewBinder {
        blueprints[blueprint.type] = AnyBlueprint(blueprint: blueprint)
        return self
    }

    func findBlueprint(type: Int) -> AnyBlueprint? {
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
        self.binder.blueprints.forEach { (entity) in
            let (_, value) = entity
            collectionView.register(value.nib, forCellWithReuseIdentifier: value.cellIdentifier)
        }
        
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
