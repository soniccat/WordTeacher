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
    private var vm: DefinitionsVM
    
    init(vm: DefinitionsVM) {
        self.vm = vm
    }

    func bind(view: V, viewItem: T) {
        let items = viewItem.items as! [DefinitionsDisplayMode]
        items.forEachIndexed { (mode, i) in
            view.segmentedControl.setTitle(mode.toStringDesc().localized(), forSegmentAt: i)
        }
        
        view.segmentedControl.selectedSegmentIndex = Int(viewItem.selectedIndex)
        
        view.segmentedControl.removeAction(event: .valueChanged)
        view.segmentedControl.setAction(event: .valueChanged) { [weak self] control in
            guard let strongSelf = self,
                let segmentControl = control as? UISegmentedControl
                else { return }
            let mode = viewItem.items[segmentControl.selectedSegmentIndex] as! DefinitionsDisplayMode
            strongSelf.vm.onDisplayModeChanged(mode: mode)
        }
        
    }
//    @objc func onSegmentChanged(sender: UISegmentedControl, forEvent event: UIEvent) {
//        vm.onDisplayModeChanged(mode: <#T##DefinitionsDisplayMode#>)
//    }
}
