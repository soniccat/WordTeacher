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
    typealias V = TextCell
    
    var type: Int {
        get {
            return Int(WordDefinitionViewItem.Companion().Type)
        }
    }
    var cellIdentifier: String {
        get {
            return "TextCell"
        }
    }
    var nibName: String {
        get {
           return "TextCell"
        }
    }

    func bind(view: TextCell, viewItem: WordDefinitionViewItem) {
        view.textView.text = viewItem.firstItem() as String? ?? ("class " + viewItem.description)
    }
}
