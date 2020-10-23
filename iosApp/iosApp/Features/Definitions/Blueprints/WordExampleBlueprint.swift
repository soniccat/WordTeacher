//
//  WordExampleBlueprint.swift
//  iosApp
//
//  Created by Alexey Glushkov on 22.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

class WordExampleBlueprint: Blueprint {
    typealias T = WordExampleViewItem
    typealias V = LabelCell
    
    var type: Int { return Int(T.Companion().Type) }

    func bind(view: V, viewItem: T) {
        view.label.applyTextAppearance(Style.wordExampleTextAppearance)
        view.label.text = viewItem.firstItem() as String?
    }
}
