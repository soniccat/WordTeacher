package com.aglushkov.wordteacher.shared.extensions

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringByAppendingPathComponent

fun NSFileManager.Companion.documentDirectory(): NSString {
    val url = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).last() as NSURL
    return url.absoluteString as NSString
}

fun NSFileManager.Companion.documentDirectoryFilePath(name: String): String {
    val dir = documentDirectory()
    return dir.stringByAppendingPathComponent(name)
}

