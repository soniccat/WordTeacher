//
//  SelfSizingCell.swift
//  iosApp
//
//  Created by Alexey Glushkov on 18.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit

class SelfSizingCell: UICollectionViewCell {
    
    var widthConstraint: NSLayoutConstraint?
    var childView: UIView? {
        get {
            return nil
        }
    }
    
    override func preferredLayoutAttributesFitting(_ layoutAttributes: UICollectionViewLayoutAttributes) -> UICollectionViewLayoutAttributes {
        updateWidthConstraint(width: layoutAttributes.size.width - contentView.layoutMargins.left - contentView.layoutMargins.right)
        let attrs = super.preferredLayoutAttributesFitting(layoutAttributes)

        // We should keep width of passed layoutAttrs here because UICollectionView
        // can't specify one dimension here and always tries to made fitting through
        // both width/height:
        // https://stackoverflow.com/questions/26143591/specifying-one-dimension-of-cells-in-uicollectionview-using-auto-layout
        
        attrs.size.width = layoutAttributes.size.width
        return attrs
    }
    
    func updateWidthConstraint(width: CGFloat) {
        if widthConstraint == nil {
            installWidthConstraint(width: width)
        } else {
            widthConstraint?.constant = width
        }
    }
    
    private func installWidthConstraint(width: CGFloat) {
        if let childView = childView {
            widthConstraint = NSLayoutConstraint.init(item: childView, attribute: .width, relatedBy: .equal, toItem: nil, attribute: .notAnAttribute, multiplier: 1.0, constant: width)
            childView.addConstraint(widthConstraint!)
        }
    }
}
