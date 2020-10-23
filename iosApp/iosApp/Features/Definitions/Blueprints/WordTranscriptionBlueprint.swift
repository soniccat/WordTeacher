//
//  WordTranscriptionBlueprint.swift
//  iosApp
//
//  Created by Alexey Glushkov on 23.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

class WordTranscriptionBlueprint: Blueprint {
    typealias T = WordTranscriptionViewItem
    typealias V = LabelCell
    
    var type: Int { return Int(T.Companion().Type) }

    func bind(view: V, viewItem: T) {
        view.label.applyTextAppearance(Style.wordTranscriptionTextAppearance)
        view.label.text = viewItem.firstItem() as String?
    }
}
