package com.airatlovesmusic.topcrop.utils

import android.graphics.RectF
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun getRectCorners(r: RectF) = floatArrayOf(
    r.left, r.top,
    r.right, r.top,
    r.right, r.bottom,
    r.left, r.bottom
)

fun getRectCenter(r: RectF) = floatArrayOf(
    r.centerX(),
    r.centerY()
)

fun getRectSidesFromCorners(corners: FloatArray): FloatArray {
    return floatArrayOf(
        sqrt((corners[0] - corners[2]).toDouble().pow(2.0) + (corners[1] - corners[3]).toDouble().pow(2.0)).toFloat(),
        sqrt((corners[2] - corners[4]).toDouble().pow(2.0) + (corners[3] - corners[5]).toDouble().pow(2.0)).toFloat()
    )
}

fun trapToRect(array: FloatArray): RectF {
    val rect = RectF(
        Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
    )
    var i = 1
    while (i < array.size) {
        val x = (array[i - 1] * 10).roundToInt() / 10f
        val y = (array[i] * 10).roundToInt() / 10f
        rect.left = if (x < rect.left) x else rect.left
        rect.top = if (y < rect.top) y else rect.top
        rect.right = if (x > rect.right) x else rect.right
        rect.bottom = if (y > rect.bottom) y else rect.bottom
        i += 2
    }
    rect.sort()
    return rect
}