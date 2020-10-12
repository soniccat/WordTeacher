package com.aglushkov.wordteacher.shared.wordteacher.extensions

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringByAppendingPathComponent

fun NSFileManager.Companion.documentDirectory(): NSString {
    return NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).last() as NSString
}

fun NSFileManager.Companion.documentDirectoryFilePath(name: String): String {
    val dir = documentDirectory()
    return dir.stringByAppendingPathComponent(name)
}

