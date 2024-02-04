package com.aglushkov.wordteacher.shared.general.okio

import okio.Path

expect fun Path.deleteIfExists(): Boolean

expect fun Path.useAsTmp(block: (Path)->Unit): Boolean