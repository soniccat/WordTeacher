//
//  File.swift
//  iosApp
//
//  Created by Alexey Glushkov on 21.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit

class DefinitionsDisplayModeCell: SelfSizingCell {
    var segmentedControl: UISegmentedControl!
    override var topMargin: CGFloat {
        Style.cellDefinitionsDisplayVerticalMargin
    }
    
    override func baseInit() {
        super.baseInit()
        segmentedControl = UISegmentedControl(frame: bounds)
        segmentedControl.translatesAutoresizingMaskIntoConstraints = false
        
        for i in 0 ..< 2 {
            segmentedControl.insertSegment(withTitle: "", at: i, animated: false)
        }
        
        contentView.addSubview(segmentedControl)
        
        setConstraintsToContentViewMargins(view: segmentedControl)
    }
}
