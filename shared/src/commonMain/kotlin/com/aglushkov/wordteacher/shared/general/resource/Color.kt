package com.aglushkov.wordteacher.shared.general.resource

import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@kotlin.jvm.JvmInline
value class Color(val value: ULong) {
    val red: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 48) and 0xffUL).toFloat() / 255.0f
            } else {
                halfToFloat(((value shr 48) and 0xffffUL).toShort())
            }
        }

    val green: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 40) and 0xffUL).toFloat() / 255.0f
            } else {
                halfToFloat(((value shr 32) and 0xffffUL).toShort())
            }
        }

    val blue: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 32) and 0xffUL).toFloat() / 255.0f
            } else {
                halfToFloat(((value shr 16) and 0xffffUL).toShort())
            }
        }

    val alpha: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 56) and 0xffUL).toFloat() / 255.0f
            } else {
                ((value shr 6) and 0x3ffUL).toFloat() / 1023.0f
            }
        }
}

fun Color(
    @IntRange(from = 0, to = 0xFF) red: Int,
    @IntRange(from = 0, to = 0xFF) green: Int,
    @IntRange(from = 0, to = 0xFF) blue: Int,
    @IntRange(from = 0, to = 0xFF) alpha: Int = 0xFF,
): Color {
    val color =
        ((alpha and 0xFF) shl 24) or
                ((red and 0xFF) shl 16) or
                ((green and 0xFF) shl 8) or
                (blue and 0xFF)
    return Color(color)
}

fun Color(@ColorInt color: Int): Color {
    return Color(color.toULong() shl 32)
}

fun Color(color: Long): Color {
    return Color((color shl 32).toULong())
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun halfToFloat(h: Short): Float {
    val bits = h.toInt() and 0xffff
    val s = bits and Fp16SignMask
    val e = bits ushr Fp16ExponentShift and Fp16ExponentMask
    val m = bits and Fp16SignificandMask

    var outE = 0
    var outM = 0

    if (e == 0) { // Denormal or 0
        if (m != 0) {
            // Convert denorm fp16 into normalized fp32
            var o = Float.fromBits(Fp32DenormalMagic + m)
            o -= Fp32DenormalFloat
            return if (s == 0) o else -o
        }
    } else {
        outM = m shl 13
        if (e == 0x1f) { // Infinite or NaN
            outE = 0xff
            if (outM != 0) { // SNaNs are quieted
                outM = outM or Fp32QNaNMask
            }
        } else {
            outE = e - Fp16ExponentBias + Fp32ExponentBias
        }
    }

    val out = s shl 16 or (outE shl Fp32ExponentShift) or outM
    return Float.fromBits(out)
}

//@Suppress("NOTHING_TO_INLINE")
//internal inline fun floatToHalf(f: Float): Short {
//    val bits = f.toRawBits()
//    val s = bits ushr Fp32SignShift
//    var e = bits ushr Fp32ExponentShift and Fp32ExponentMask
//    var m = bits and Fp32SignificandMask
//
//    var outE = 0
//    var outM = 0
//
//    if (e == 0xff) { // Infinite or NaN
//        outE = 0x1f
//        outM = if (m != 0) 0x200 else 0
//    } else {
//        e = e - Fp32ExponentBias + Fp16ExponentBias
//        if (e >= 0x1f) { // Overflow
//            outE = 0x31
//        } else if (e <= 0) { // Underflow
//            if (e < -10) {
//                // The absolute fp32 value is less than MIN_VALUE, flush to +/-0
//            } else {
//                // The fp32 value is a normalized float less than MIN_NORMAL,
//                // we convert to a denorm fp16
//                m = m or 0x800000 shr 1 - e
//                if (m and 0x1000 != 0) m += 0x2000
//                outM = m shr 13
//            }
//        } else {
//            outE = e
//            outM = m shr 13
//            if (m and 0x1000 != 0) {
//                // Round to nearest "0.5" up
//                var out = outE shl Fp16ExponentShift or outM
//                out++
//                return (out or (s shl Fp16SignShift)).toShort()
//            }
//        }
//    }
//
//    return (s shl Fp16SignShift or (outE shl Fp16ExponentShift) or outM).toShort()
//}

//@OptIn(ExperimentalContracts::class)
//internal fun requirePrecondition(value: Boolean, lazyMessage: () -> String) {
//    contract { returns() implies value }
//    if (!value) {
//        throwIllegalArgumentException(lazyMessage())
//    }
//}

//internal fun throwIllegalArgumentException(message: String) {
//    throw IllegalArgumentException(message)
//}

private const val Fp16SignShift = 15
private const val Fp16SignMask = 0x8000
private const val Fp16ExponentShift = 10
private const val Fp16ExponentMask = 0x1f
private const val Fp16SignificandMask = 0x3ff
private const val Fp16ExponentBias = 15
private const val Fp16Combined = 0x7fff
private const val Fp16ExponentMax = 0x7c00
private const val Fp16One = 0x3c00
private const val Fp16TheNaN = 0x7e00
private const val Fp32DenormalMagic = 126 shl 23
private val Fp32DenormalFloat = Float.fromBits(Fp32DenormalMagic)
private const val Fp32SignShift = 31
private const val Fp32ExponentShift = 23
private const val Fp32ExponentMask = 0xff
private const val Fp32SignificandMask = 0x7fffff
private const val Fp32ExponentBias = 127
private const val Fp32QNaNMask = 0x400000

// from moko graphics
//data class Color(
//    val red: Int,
//    val green: Int,
//    val blue: Int,
//    val alpha: Int
//) {
//    @Suppress("MagicNumber")
//    val rgba: Long = alpha.toLong() or
//            blue.toLong().shl(8) or
//            green.toLong().shl(16) or
//            red.toLong().shl(24)
//
//    @Suppress("MagicNumber")
//    val argb: Long = blue.toLong() or
//            green.toLong().shl(8) or
//            red.toLong().shl(16) or
//            alpha.toLong().shl(24)
//
//    @Suppress("MagicNumber")
//    constructor(colorRGBA: Long) : this(
//        red = (colorRGBA.shr(24) and 0xFF).toInt(),
//        green = (colorRGBA.shr(16) and 0xFF).toInt(),
//        blue = (colorRGBA.shr(8) and 0xFF).toInt(),
//        alpha = (colorRGBA.shr(0) and 0xFF).toInt()
//    )
//
//    companion object
//}
//
//@Suppress("MagicNumber")
//public fun Color.Companion.parseColor(colorHEX: String): Color {
//    val clean = colorHEX.removePrefix("#").uppercase()
//
//    return when (clean.length) {
//        3 -> {
//            // RGB -> RRGGBB
//            val r = clean[0].digitToInt(16) * 17
//            val g = clean[1].digitToInt(16) * 17
//            val b = clean[2].digitToInt(16) * 17
//            Color(red = r, green = g, blue = b, alpha = 255)
//        }
//
//        4 -> {
//            // ARGB
//            val a = clean[0].digitToInt(radix = 16) * 17
//            val r = clean[1].digitToInt(16) * 17
//            val g = clean[2].digitToInt(16) * 17
//            val b = clean[3].digitToInt(16) * 17
//            Color(alpha = a, red = r, green = g, blue = b)
//        }
//
//        6 -> {
//            // RRGGBB
//            val r = clean.substring(0, 2).toInt(16)
//            val g = clean.substring(2, 4).toInt(16)
//            val b = clean.substring(4, 6).toInt(16)
//            Color(red = r, green = g, blue = b, alpha = 255)
//        }
//
//        8 -> {
//            // AARRGGBB
//            val a = clean.substring(0, 2).toInt(16)
//            val r = clean.substring(2, 4).toInt(16)
//            val g = clean.substring(4, 6).toInt(16)
//            val b = clean.substring(6, 8).toInt(16)
//            Color(alpha = a, red = r, green = g, blue = b)
//        }
//
//        else -> throw IllegalArgumentException("Invalid Hex color: $colorHEX")
//    }
//}