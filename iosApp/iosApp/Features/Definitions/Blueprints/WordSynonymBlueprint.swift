//
//  WordSynonymBlueprint.swift
//  iosApp
//
//  Created by Alexey Glushkov on 23.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

class WordSynonymBlueprint: Blueprint {
    typealias T = WordSynonymViewItem
    typealias V = LabelCell
    
    var type: Int { return Int(T.Companion().Type) }

    func bind(view: V, viewItem: T) {
        view.label.applyTextAppearance(Style.wordSynonymTextAppearance)
        view.label.text = viewItem.firstItem() as String?
    }
}

