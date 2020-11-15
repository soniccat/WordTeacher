//
//  DividerCell.swift
//  iosApp
//
//  Created by Alexey Glushkov on 23.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit

class DividerCell: SelfSizingCell {
    var dividerView: UIView!
    override var topMargin: CGFloat {
        Style.cellDividerTopMargin
    }
    override var bottomMargin: CGFloat {
        Style.cellDividerBottomMargin
    }
    
    override func baseInit() {
        super.baseInit()
        
        dividerView = UIView(frame: bounds)
        dividerView.translatesAutoresizingMaskIntoConstraints = false
        dividerView.backgroundColor = Style.dividerColor
        contentView.addSubview(dividerView)
        dividerView.heightAnchor.constraint(equalToConstant: Style.dividerHeight).isActive = true
        
        setConstraintsToContentViewMargins(view: dividerView)
    }
}

