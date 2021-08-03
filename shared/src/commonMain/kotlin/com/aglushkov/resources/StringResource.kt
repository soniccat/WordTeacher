/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resources

import com.aglushkov.resources.desc.ResourceFormatted
import com.aglushkov.resources.desc.StringDesc

expect class StringResource

@Suppress("SpreadOperator")
fun StringResource.format(vararg args: Any) = StringDesc.ResourceFormatted(this, *args)
fun StringResource.format(args: List<Any>) = StringDesc.ResourceFormatted(this, args)
