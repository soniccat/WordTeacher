//
//  UICollectionViewCell.swift
//  iosApp
//
//  Created by Alexey Glushkov on 22.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit

extension UICollectionViewCell {
    func setConstraintsToContentViewMargins(view: UIView) {
        [
            view.topAnchor.constraint(equalTo: contentView.layoutMarginsGuide.topAnchor),
            view.leftAnchor.constraint(equalTo: contentView.layoutMarginsGuide.leftAnchor),
            view.rightAnchor.constraint(equalTo: contentView.layoutMarginsGuide.rightAnchor),
            view.bottomAnchor.constraint(equalTo: contentView.layoutMarginsGuide.bottomAnchor)
        ].activate()
    }
}
