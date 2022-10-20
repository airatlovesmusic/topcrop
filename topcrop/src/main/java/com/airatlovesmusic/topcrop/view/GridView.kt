package com.airatlovesmusic.topcrop.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.airatlovesmusic.topcrop.R

class GridView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    private val cropViewRect = RectF()
    private val tmpRect = RectF()

    private var currentWidth = 0
    private var currentHeight = 0

    private var cropGridCorners = floatArrayOf()
    private var cropGridCenter = floatArrayOf()

    private var rowsCount = 2
    private var columnsCount = 2
    private var targetAspectRatio = 0f
    private var gridPoints: FloatArray? = null

    private val dimColor by lazy { resources.getColor(R.color.dim_color) }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = resources.displayMetrics.densityDpi.toFloat()
        color = resources.getColor(R.color.grid_color)
    }

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = resources.displayMetrics.densityDpi.toFloat()
        color = resources.getColor(R.color.crop_frame_color)
        style = Paint.Style.STROKE
    }

    private var previousTouchX = -1f
    private var previousTouchY = -1f
    private var currentTouchCornerIndex = -1
    private val touchPointThreshold by lazy { resources.displayMetrics.densityDpi * 30 }
    private val cropRectMinSize by lazy { resources.displayMetrics.densityDpi * 100 }
    private var setUpCropBoundsLater = false

    fun setTargetAspectRatio(targetAspectRatio: Float) {
        this.targetAspectRatio = targetAspectRatio
        if (currentWidth > 0) {
            setupCropBounds()
            postInvalidate()
        } else setUpCropBoundsLater = true
    }

    private fun setupCropBounds() {
        val height = (currentWidth / targetAspectRatio).toInt()
        if (height > currentHeight) {
            val width = (currentHeight * targetAspectRatio).toInt()
            val halfDiff = (currentWidth - width) / 2
            cropViewRect[(paddingLeft + halfDiff).toFloat(), paddingTop.toFloat(), (
                paddingLeft + width + halfDiff).toFloat()] = (paddingTop + currentHeight).toFloat()
        } else {
            val halfDiff = (currentHeight - height) / 2
            cropViewRect[paddingLeft.toFloat(), (paddingTop + halfDiff).toFloat(), (
                paddingLeft + currentWidth).toFloat()] = (paddingTop + height + halfDiff).toFloat()
        }
        updateGridPoints()
    }

    private fun updateGridPoints() {
        cropGridCorners = getRectCorners(cropViewRect)
        cropGridCenter = getRectCenter(cropViewRect)
        gridPoints = null
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            currentWidth = width - paddingRight - paddingLeft
            currentHeight = height - paddingBottom - paddingTop
            if (setUpCropBoundsLater) {
                setUpCropBoundsLater = false
                setTargetAspectRatio(targetAspectRatio)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawDimmedLayer(canvas)
        drawCropGrid(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (cropViewRect.isEmpty) return false
        var x = event.x
        var y = event.y
        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_DOWN) {
            currentTouchCornerIndex = getCurrentTouchIndex(x, y)
            val shouldHandle = currentTouchCornerIndex != -1
            if (!shouldHandle) {
                previousTouchX = -1f
                previousTouchY = -1f
            } else if (previousTouchX < 0) {
                previousTouchX = x
                previousTouchY = y
            }
            return shouldHandle
        }
        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_MOVE) {
            if (event.pointerCount == 1 && currentTouchCornerIndex != -1) {
                x = Math.min(Math.max(x, paddingLeft.toFloat()), (width - paddingRight).toFloat())
                y = Math.min(Math.max(y, paddingTop.toFloat()), (height - paddingBottom).toFloat())
                updateCropViewRect(x, y)
                previousTouchX = x
                previousTouchY = y
                return true
            }
        }
        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP) {
            previousTouchX = -1f
            previousTouchY = -1f
            currentTouchCornerIndex = -1
//          onCropRectUpdated(cropViewRect)
        }
        return false
    }

    private fun updateCropViewRect(touchX: Float, touchY: Float) {
        tmpRect.set(cropViewRect)
        when (currentTouchCornerIndex) {
            0 -> tmpRect[touchX, touchY, cropViewRect.right] =
                cropViewRect.bottom
            1 -> tmpRect[cropViewRect.left, touchY, touchX] = cropViewRect.bottom
            2 -> tmpRect[cropViewRect.left, cropViewRect.top, touchX] = touchY
            3 -> tmpRect[touchX, cropViewRect.top, cropViewRect.right] = touchY
            4 -> {
                tmpRect.offset(touchX - previousTouchX, touchY - previousTouchY)
                if (tmpRect.left > left && tmpRect.top > top && tmpRect.right < right && tmpRect.bottom < bottom) {
                    cropViewRect.set(tmpRect)
                    updateGridPoints()
                    postInvalidate()
                }
                return
            }
        }
        val changeHeight = tmpRect.height() >= cropRectMinSize
        val changeWidth = tmpRect.width() >= cropRectMinSize
        cropViewRect[if (changeWidth) tmpRect.left else cropViewRect.left, if (changeHeight) tmpRect.top else cropViewRect.top, if (changeWidth) tmpRect.right else cropViewRect.right] =
            if (changeHeight) tmpRect.bottom else cropViewRect.bottom
        if (changeHeight || changeWidth) {
            updateGridPoints()
            postInvalidate()
        }
    }

    private fun getCurrentTouchIndex(touchX: Float, touchY: Float): Int {
        var closestPointIndex = -1
        var closestPointDistance = touchPointThreshold.toDouble()
        var i = 0
        while (i < 8) {
            val distanceToCorner = Math.sqrt(
                Math.pow((touchX - cropGridCorners[i]).toDouble(), 2.0)
                    + Math.pow((touchY - cropGridCorners[i + 1]).toDouble(), 2.0)
            )
            if (distanceToCorner < closestPointDistance) {
                closestPointDistance = distanceToCorner
                closestPointIndex = i / 2
            }
            i += 2
        }
        return closestPointIndex
    }

    private fun drawDimmedLayer(canvas: Canvas) {
        canvas.save()
        canvas.clipRect(cropViewRect, Region.Op.DIFFERENCE)
        canvas.drawColor(dimColor)
        canvas.restore()
    }

    private fun drawCropGrid(canvas: Canvas) {
        if (gridPoints == null && !cropViewRect.isEmpty) {
            val gridPoints = FloatArray((rowsCount) * 4 + (columnsCount) * 4)
            var index = 0
            for (i in 0 until rowsCount) {
                gridPoints[index++] = cropViewRect.left
                gridPoints[index++] = (cropViewRect.height() * ((i.toFloat() + 1.0f) / (rowsCount + 1).toFloat())) + cropViewRect.top
                gridPoints[index++] = cropViewRect.right
                gridPoints[index++] = (cropViewRect.height() * ((i.toFloat() + 1.0f) / (rowsCount + 1).toFloat())) + cropViewRect.top
            }
            for (i in 0 until columnsCount) {
                gridPoints[index++] = (cropViewRect.width() * ((i.toFloat() + 1.0f) / (columnsCount + 1).toFloat())) + cropViewRect.left
                gridPoints[index++] = cropViewRect.top
                gridPoints[index++] = (cropViewRect.width() * ((i.toFloat() + 1.0f) / (columnsCount + 1).toFloat())) + cropViewRect.left
                gridPoints[index++] = cropViewRect.bottom
            }
            this.gridPoints = gridPoints
        }
        gridPoints?.let { canvas.drawLines(it, gridPaint) }
        canvas.drawRect(cropViewRect, framePaint)
    }

    private fun getRectCorners(r: RectF) = floatArrayOf(
        r.left, r.top,
        r.right, r.top,
        r.right, r.bottom,
        r.left, r.bottom
    )

    private fun getRectCenter(r: RectF) = floatArrayOf(
        r.centerX(),
        r.centerY()
    )

}