//
//  WordDefinitionBlueprint.swift
//  iosApp
//
//  Created by Alexey Glushkov on 21.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

class WordDefinitionBlueprint: Blueprint<TextCell, WordDefinitionViewItem, NSString> {
    override var type: Int {
        get {
            return Int(WordDefinitionViewItem.Companion().Type)
        }
    }
    override var cellIdentifier: String {
        get {
            return "TextCell"
        }
    }
    override var nibName: String {
        get {
           return "TextCell"
        }
    }

    override func bind(view: TextCell, viewItem: WordDefinitionViewItem) {
        
    }
}
