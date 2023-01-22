package com.aglushkov.wordteacher.android_app.general

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