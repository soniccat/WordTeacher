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
    
    override func baseInit() {
        dividerView = UIView(frame: bounds)
        dividerView.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(dividerView)
        
        dividerView.heightAnchor.constraint(equalToConstant: Style.dividerHeight).isActive = true
        setConstraintsToContentViewMargins(
            view: dividerView,
            insets: UIEdgeInsets(top: 0, left: Style.dividerLeftMargin, bottom: 0, right: 0))
    }
}

