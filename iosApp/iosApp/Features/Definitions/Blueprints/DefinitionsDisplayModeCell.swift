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
    
    override func baseInit() {
        // TODO: apply style from Design
        segmentedControl = UISegmentedControl(frame: bounds)
        segmentedControl.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(segmentedControl)
        
        setConstraintsToContentViewMargins(view: segmentedControl)
    }
}
