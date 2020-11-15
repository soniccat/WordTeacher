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
    var topMargin: CGFloat {
        Style.cellTopMargin
    }
    var bottomMargin: CGFloat {
        Style.cellBottomMargin
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        baseInit()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        baseInit()
    }
    
    func baseInit() {
        // Set autoresizingMask to avoid zero autoresizingMask which will lead to having a wrong width of the contentView in preferredLayoutAttributesFitting, so self-sizing will work buggy
        // More details: https://stackoverflow.com/questions/24750158/autoresizing-issue-of-uicollectionviewcell-contentviews-frame-in-storyboard-pro
        contentView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        updateMargins()
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        updateMargins()
    }
    
    private func updateMargins() {
        contentView.layoutMargins.top = topMargin
        contentView.layoutMargins.bottom = bottomMargin
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
        
        for constraint in contentView.constraints {
            if constraint.firstAttribute == .width && constraint != widthConstraint && constraint.firstItem === self.contentView {
                // to disable auto added UIView-Encapsulated-Layout-Width
                constraint.priority = UILayoutPriority.defaultLow

            } else if constraint.firstAttribute == .height && constraint.firstItem === self.contentView {
                // to disable auto added UIView-Encapsulated-Layout-Height
                constraint.priority = UILayoutPriority.defaultLow
            }
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
