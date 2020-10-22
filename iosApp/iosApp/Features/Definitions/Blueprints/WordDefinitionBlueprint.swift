//
//  WordDefinitionBlueprint.swift
//  iosApp
//
//  Created by Alexey Glushkov on 21.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

class WordDefinitionBlueprint: Blueprint {
    typealias T = WordDefinitionViewItem
    typealias V = LabelCell
    
    var type: Int { return Int(T.Companion().Type) }

    func bind(view: V, viewItem: T) {
        view.textView.text = viewItem.firstItem() as String?
    }
}
