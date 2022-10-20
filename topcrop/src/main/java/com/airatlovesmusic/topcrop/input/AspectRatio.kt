package com.airatlovesmusic.topcrop.input

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

const val ASPECT_RATIO_ORIGINAL = -1f

@Parcelize
data class AspectRatio(
    val x: Float = ASPECT_RATIO_ORIGINAL,
    val y: Float = ASPECT_RATIO_ORIGINAL,
    val title: String = "$x:$y"
): Parcelable