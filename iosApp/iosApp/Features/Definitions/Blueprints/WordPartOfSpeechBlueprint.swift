//
//  WordPartOfSpeechBlueprint.swift
//  iosApp
//
//  Created by Alexey Glushkov on 23.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

class WordPartOfSpeechBlueprint: Blueprint {
    typealias T = WordPartOfSpeechViewItem
    typealias V = LabelCell
    
    var type: Int { return Int(T.Companion().Type) }

    func bind(view: V, viewItem: T) {
        view.contentView.layoutMargins.top = Style.cellPartOfSpeechTopMargin
        view.contentView.layoutMargins.bottom = Style.cellHeaderBottomMargin
        view.label.applyTextAppearance(Style.wordPartOfSpeechTextAppearance)
        view.label.text = viewItem.firstItem()?.localized()
    }
}
