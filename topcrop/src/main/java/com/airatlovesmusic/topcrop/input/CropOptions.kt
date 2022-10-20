package com.airatlovesmusic.topcrop.input

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class CropOptions internal constructor(
    val aspectRatios: List<AspectRatio>
): Parcelable {

    class Builder() {
        private val aspectRatios = mutableListOf<AspectRatio>()

        fun addAspectRatio(aspectRatio: AspectRatio) = apply { aspectRatios.add(aspectRatio) }
        fun addAspectRatios(list: List<AspectRatio>) = apply { aspectRatios.addAll(aspectRatios) }
        fun addAspectRatios(vararg aspectRatios: AspectRatio) = apply { this.aspectRatios.addAll(aspectRatios) }
        fun build(): CropOptions {
            require(aspectRatios.isNotEmpty()) { "Please set at least one aspect ratio" }
            return CropOptions(aspectRatios)
        }
    }

}