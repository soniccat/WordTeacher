//
//  ItemViewBinder.swift
//  iosApp
//
//  Created by Alexey Glushkov on 21.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import shared

class ItemViewBinder {
    var blueprints = Dictionary<Int, AnyBlueprint>()

    func addBlueprint(blueprint: BaseBlueprint) -> ItemViewBinder {
        blueprints[blueprint.type] = AnyBlueprint(blueprint: blueprint)
        return self
    }

    func findBlueprint(type: Int) -> AnyBlueprint? {
        return blueprints[type]
    }
}
