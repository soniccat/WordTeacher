/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources

import platform.Foundation.NSBundle

actual interface ResourceContainer<T> {
    val nsBundle: NSBundle
}
