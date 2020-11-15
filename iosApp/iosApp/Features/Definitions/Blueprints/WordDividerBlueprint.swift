//
//  WordDividerBlueprint.swift
//  iosApp
//
//  Created by Alexey Glushkov on 23.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

class WordDividerBlueprint: Blueprint {
    typealias T = WordDividerViewItem
    typealias V = DividerCell
    
    var type: Int { return Int(T.Companion().Type) }

    func bind(view: DividerCell, viewItem: WordDividerViewItem) {
    }
}
