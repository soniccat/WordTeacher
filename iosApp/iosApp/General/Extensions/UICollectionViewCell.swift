//
//  UICollectionViewCell.swift
//  iosApp
//
//  Created by Alexey Glushkov on 22.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit

extension UICollectionViewCell {
    func setConstraintsToContentViewMargins(view: UIView, insets: UIEdgeInsets = UIEdgeInsets.zero) {
        [
            view.topAnchor.constraint(equalTo: contentView.layoutMarginsGuide.topAnchor, constant: insets.top),
            view.leftAnchor.constraint(equalTo: contentView.layoutMarginsGuide.leftAnchor, constant: insets.left),
            view.rightAnchor.constraint(equalTo: contentView.layoutMarginsGuide.rightAnchor, constant: insets.right),
            view.bottomAnchor.constraint(equalTo: contentView.layoutMarginsGuide.bottomAnchor, constant: insets.bottom)
        ].activate()
    }
}
