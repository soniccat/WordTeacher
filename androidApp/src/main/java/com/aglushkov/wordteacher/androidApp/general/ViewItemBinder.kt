package com.aglushkov.wordteacher.androidApp.general

class ViewItemBinder {
    private val blueprints = mutableMapOf<Int, Blueprint<*,*>>()

    fun addBlueprint(blueprint: Blueprint<*,*>): ViewItemBinder {
        blueprints[blueprint.type] = blueprint
        return this
    }

    fun findBlueprint(type: Int): Blueprint<*,*>? {
        return blueprints[type]
    }
}