package com.sample.kmmsharedmodule

import platform.UIKit.UIDevice
//import cocoapods.AFNetworking.*

actual class Platform actual constructor() {
    actual val platform: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

//    fun test() {
//        val v = AFHTTPRequestQueryStringDefaultStyle
//    }
}