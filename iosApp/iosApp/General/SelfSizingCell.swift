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
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        baseInit()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        baseInit()
    }
    
    func baseInit() {
        
    }
    
    override func preferredLayoutAttributesFitting(_ layoutAttributes: UICollectionViewLayoutAttributes) -> UICollectionViewLayoutAttributes {
        updateWidthConstraint(width: layoutAttributes.size.width)
        let attrs = super.preferredLayoutAttributesFitting(layoutAttributes)

        // We should keep width of passed layoutAttrs here because UICollectionView
        // can't specify one dimension and always tries to made fitting through
        // both width and height:
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
        widthConstraint = NSLayoutConstraint.init(
            item: contentView,
            attribute: .width,
            relatedBy: .equal,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1.0,
            constant: width
        )
        contentView.addConstraint(widthConstraint!)
    }
}
