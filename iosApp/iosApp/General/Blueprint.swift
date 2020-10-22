//
//  Blueprint.swift
//  iosApp
//
//  Created by Alexey Glushkov on 21.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit

// Type-erased Blueprint
protocol BaseBlueprint {
    var type: Int { get }
    var cellIdentifier: String { get }
    var cellClass: AnyClass? { get }
    var nibName: String? { get }

    func bind(view: UIView, viewItem: Any)
}

extension BaseBlueprint {
    var nibName: String? {
        return cellIdentifier
    }
    
    var nib: UINib? {
        if let nibName = nibName {
            return UINib(nibName: nibName, bundle: nil)
        } else {
            return nil
        }
    }
    
    var cellClass: AnyClass? {
        return nil
    }
    
    func register(collectionView: UICollectionView) {
        assert(nib != nil || cellClass != nil, "The nib or the cellClass should be nonnull")
        if let nib = self.nib {
            collectionView.register(nib, forCellWithReuseIdentifier: cellIdentifier)
        } else {
            collectionView.register(self.cellClass!, forCellWithReuseIdentifier: cellIdentifier)
        }
    }
}

protocol Blueprint: BaseBlueprint {
    associatedtype T: Any
    associatedtype V: UIView

    func bind(view: V, viewItem: T)
}

extension Blueprint {
    var cellIdentifier: String { return String(describing: V.self) }
    var cellClass: AnyClass? { return V.self }
    
    // forward to the typed version
    func bind(view: UIView, viewItem: Any) {
        if V.self != UIView.self || T.self != Any.self {
            bind(view: view as! V, viewItem: viewItem as! T)
        }
    }
}

// Container to be able to store different blueprints in a collection
class AnyBlueprint: BaseBlueprint {
    private var blueprint: BaseBlueprint
    
    var type: Int { blueprint.type }
    var cellIdentifier: String { blueprint.cellIdentifier }
    var nibName: String? { blueprint.nibName }
    var nib: UINib? { blueprint.nib }
    var cellClass: AnyClass? { blueprint.cellClass }
    
    init(blueprint: BaseBlueprint) {
        self.blueprint = blueprint
    }
    
    func bind(view: UIView, viewItem: Any) {
        self.blueprint.bind(view: view, viewItem: viewItem)
    }
}
