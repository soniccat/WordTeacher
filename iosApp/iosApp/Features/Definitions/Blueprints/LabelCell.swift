//
//  TextCell.swift
//  iosApp
//
//  Created by Alexey Glushkov on 18.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit

class LabelCell: SelfSizingCell {
    var label: UILabel!
    
    override func baseInit() {
        super.baseInit()
        
        label = UILabel(frame: bounds)
        label.translatesAutoresizingMaskIntoConstraints = false
        label.numberOfLines = 0
        contentView.addSubview(label)
        
        setConstraintsToContentViewMargins(view: label)
    }
}
