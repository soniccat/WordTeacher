//
//  DefinitionsDisplayModeBlueprint.swift
//  iosApp
//
//  Created by Alexey Glushkov on 21.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

class DefinitionsDisplayModeBlueprint: Blueprint {
    typealias T = DefinitionsDisplayModeViewItem
    typealias V = DefinitionsDisplayModeCell
    
    var type: Int { return Int(T.Companion().Type) }
    
    init(vm: DefinitionsVM) {
    }

    func bind(view: V, viewItem: T) {
        let items = viewItem.items as! [DefinitionsDisplayMode]
        items.forEachIndexed { (mode, i) in
            view.segmentedControl.setTitle(mode.toStringDesc().localized(), forSegmentAt: i)
        }
        
        view.segmentedControl.selectedSegmentIndex = Int(viewItem.selectedIndex)
    }
}
