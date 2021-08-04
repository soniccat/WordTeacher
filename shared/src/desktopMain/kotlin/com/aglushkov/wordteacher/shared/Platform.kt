package com.aglushkov.wordteacher.shared

actual class Platform actual constructor() {
    actual val platform: String = "Desktop ${android.os.Build.VERSION.SDK_INT}"
}