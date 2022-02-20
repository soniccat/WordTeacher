package com.aglushkov.wordteacher.shared.general

public data class Quadruple<out A, out B, out C, out D>(
    public val first: A,
    public val second: B,
    public val third: C,
    public val forth: D
)