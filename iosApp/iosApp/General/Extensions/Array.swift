//
//  Array.swift
//  iosApp
//
//  Created by Alexey Glushkov on 15.11.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import Foundation

extension Array {
    func forEachIndexed(_ body: (Element, Index) throws -> Void) rethrows {
        for i in 0 ..< count {
            try body(self[i], i)
        }
    }
}
