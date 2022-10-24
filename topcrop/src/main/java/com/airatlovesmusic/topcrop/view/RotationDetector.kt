package com.airatlovesmusic.topcrop.view

import android.graphics.PointF
import android.view.MotionEvent
import kotlin.math.atan2

class RotationDetector(private val listener: (Float) -> Unit) {

    private var isFirstTouch = false
    private var pointer1: Int = INVALID_POINTER_INDEX
    private var pointer2: Int = INVALID_POINTER_INDEX

    private var pointer1Pos = PointF(0f, 0f)
    private var pointer2Pos = PointF(0f, 0f)
    private var angle = 0f

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointer1Pos.set(event.x, event.y)
                pointer1 = event.findPointerIndex(event.getPointerId(0))
                angle = 0f
                isFirstTouch = true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                pointer2Pos.set(event.x, event.y)
                pointer2 = event.findPointerIndex(event.getPointerId(event.actionIndex))
                angle = 0f
                isFirstTouch = true
            }
            MotionEvent.ACTION_MOVE -> if (pointer1 != INVALID_POINTER_INDEX && pointer2 != INVALID_POINTER_INDEX && event.pointerCount > pointer2) {
                val newPointer1Pos = PointF(event.getX(pointer1), event.getY(pointer1))
                val newPointer2Pos = PointF(event.getX(pointer2), event.getY(pointer2))
                angle = if (isFirstTouch) { isFirstTouch = false; 0f }
                else calculateAngleBetweenLines(newPointer1Pos, newPointer2Pos)
                listener.invoke(angle)
                pointer1Pos = newPointer1Pos
                pointer2Pos = newPointer2Pos
            }
            MotionEvent.ACTION_UP -> pointer1 = INVALID_POINTER_INDEX
            MotionEvent.ACTION_POINTER_UP -> pointer2 = INVALID_POINTER_INDEX
        }
        return true
    }

    private fun calculateAngleBetweenLines(
        newPointer1Pos: PointF,
        newPointer2Pos: PointF,
    ): Float {
        val angleFrom = Math.toDegrees(atan2((pointer2Pos.y - pointer1Pos.y).toDouble(), (pointer2Pos.x - pointer1Pos.x).toDouble()).toFloat().toDouble()).toFloat()
        val angleTo = Math.toDegrees(atan2((newPointer2Pos.y - newPointer1Pos.y).toDouble(), (newPointer2Pos.x - newPointer1Pos.x).toDouble()).toFloat().toDouble()).toFloat()
        val angle = angleTo % 360.0f - angleFrom % 360.0f

        return if (angle < -180.0f) angle + 360.0f
        else if (angle > 180.0f) angle - 360.0f
        else angle
    }

    companion object {
        private const val INVALID_POINTER_INDEX = -1
    }
}