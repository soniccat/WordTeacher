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
    var nibName: String { get }
    var nib: UINib { get }

    func bind(view: UIView, viewItem: Any)
}

extension BaseBlueprint {
    var nib: UINib {
        get {
            return UINib(nibName: nibName, bundle: nil)
        }
    }
}

protocol Blueprint: BaseBlueprint {
    associatedtype T: Any
    associatedtype V: UIView
    
    var type: Int { get }
    var cellIdentifier: String { get }
    var nibName: String { get }

    func bind(view: V, viewItem: T)
}

extension Blueprint {
    func bind(view: UIView, viewItem: Any) {
        bind(view: view as! V, viewItem: viewItem as! T)
    }
}

class AnyBlueprint: BaseBlueprint {
    private var blueprint: BaseBlueprint
    
    var type: Int { get { blueprint.type } }
    var cellIdentifier: String { get { blueprint.cellIdentifier } }
    var nibName: String { get { blueprint.nibName } }
    var nib: UINib { get { blueprint.nib } }
    
    init(blueprint: BaseBlueprint) {
        self.blueprint = blueprint
    }
    
    func bind(view: UIView, viewItem: Any) {
        self.blueprint.bind(view: view, viewItem: viewItem)
    }
}
