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

    func bind(view: V, viewItem: T) {
        view.contentView.layoutMargins.top = Style.cellDefinitionsDisplayVerticalMargin
        
        let items = viewItem.items as! [DefinitionsDisplayMode]
        view.segmentedControl.removeAllSegments()

        items.forEach { item in
            view.segmentedControl.insertSegment(
                withTitle: item.toStringDesc().localized(),
                at: view.segmentedControl.numberOfSegments,
                animated: false
            )
        }
        
        var seletedIndex = 0
        switch viewItem.selected {
            case .merged: seletedIndex = 1
            default: seletedIndex = 0
        }
        
        view.segmentedControl.selectedSegmentIndex = seletedIndex
    }
}
