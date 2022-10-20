package com.airatlovesmusic.topcrop.view

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import androidx.appcompat.widget.AppCompatImageView

open class TopCropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): AppCompatImageView(context, attrs, defStyle) {

    private var mScaleDetector: ScaleGestureDetector? = null
    private var mGestureDetector: GestureDetector? = null
    private var middlePointX = 0f
    private var middlePointY = 0f

    var targetAspectRatio: Float = 3f / 4f
    private var currentImageMatrix = Matrix()

    init {
        mGestureDetector = GestureDetector(context, GestureListener(), null, true)
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        scaleType = ScaleType.MATRIX
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1) {
            middlePointX = (event.getX(0) + event.getX(1)) / 2
            middlePointY = (event.getY(0) + event.getY(1)) / 2
        }
        mGestureDetector?.onTouchEvent(event)
        mScaleDetector?.onTouchEvent(event)
        return true
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            postScale(detector.scaleFactor, middlePointX, middlePointY)
            return true
        }
    }

    private fun postScale(deltaScale: Float, px: Float, py: Float) {
        if (deltaScale != 0f) {
            currentImageMatrix.postScale(deltaScale, deltaScale, px, py)
            imageMatrix = currentImageMatrix
        }
    }

    private fun postTranslate(deltaX: Float, deltaY: Float) {
        if (deltaX != 0f || deltaY != 0f) {
            currentImageMatrix.postTranslate(deltaX, deltaY)
            imageMatrix = currentImageMatrix
        }
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            postTranslate(-distanceX, -distanceY)
            return true
        }
    }

}