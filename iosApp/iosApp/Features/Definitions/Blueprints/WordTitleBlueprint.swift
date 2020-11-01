//
//  WordTitleBlueprint.swift
//  iosApp
//
//  Created by Alexey Glushkov on 23.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

class WordTitleBlueprint: Blueprint {
    typealias T = WordTitleViewItem
    typealias V = LabelCell
    
    var type: Int { return Int(T.Companion().Type) }

    func bind(view: V, viewItem: T) {
        view.contentView.layoutMargins.top = Style.cellExtraTopMargin
        view.label.applyTextAppearance(Style.wordTitleTextAppearance)
        view.label.text = viewItem.firstItem() as String?
        // TODO: show providedBy
    }
}
