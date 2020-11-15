//
//  UIFontDescriptor.swift
//  iosApp
//
//  Created by Alexey Glushkov on 01.11.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit

extension UIFontDescriptor {
    func toSemibold() -> UIFontDescriptor {
        return addingAttributes(
            [UIFontDescriptor.AttributeName.traits :
                [UIFontDescriptor.TraitKey.weight : UIFont.Weight.semibold]
            ]
        )
    }
    
    func toFont() -> UIFont {
        return UIFont(descriptor: self, size: pointSize)
    }
}
