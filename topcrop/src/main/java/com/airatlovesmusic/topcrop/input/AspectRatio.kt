package com.airatlovesmusic.topcrop.input

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AspectRatio(
    val x: Float = 3F,
    val y: Float = 4F,
    val title: String = "${y.toInt()}:${x.toInt()}"
): Parcelable