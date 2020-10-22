//
//  TextCell.swift
//  iosApp
//
//  Created by Alexey Glushkov on 18.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit

class LabelCell: SelfSizingCell {
    var textView: UILabel!
    
    override func baseInit() {
        // TODO: apply style from Design
        textView = UILabel(frame: bounds)
        textView.translatesAutoresizingMaskIntoConstraints = false
        textView.numberOfLines = 0
        contentView.addSubview(textView)
        
        setConstraintsToContentViewMargins(view: textView)
    }
}
