package com.aglushkov.wordteacher.shared


import platform.UIKit.UIDevice
import cocoapods.AFNetworking.*

actual class Platform actual constructor() {
    actual val platform: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    fun test() {
        val v = AFURLSessionManager()
    }
}