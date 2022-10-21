package com.airatlovesmusic.topcrop.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Region
import android.util.AttributeSet
import android.view.View
import com.airatlovesmusic.topcrop.R

class GridView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val cropViewRect = RectF()
    private var currentWidth = 0
    private var currentHeight = 0
    private var targetAspectRatio = 0f
    private var gridPoints: FloatArray? = null

    private var setUpCropBoundsLater = false

    var listener: Listener? = null

    private val dimColor by lazy { resources.getColor(R.color.dim_color) }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = resources.displayMetrics.density
        color = resources.getColor(R.color.grid_color)
    }

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = resources.displayMetrics.density
        color = resources.getColor(R.color.crop_frame_color)
        style = Paint.Style.STROKE
    }

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
            cropViewRect[
                (paddingLeft + halfDiff).toFloat(),
                paddingTop.toFloat(),
                (paddingLeft + width + halfDiff).toFloat()
            ] = (paddingTop + currentHeight).toFloat()
        } else {
            val halfDiff = (currentHeight - height) / 2
            cropViewRect[
                paddingLeft.toFloat(),
                (paddingTop + halfDiff).toFloat(),
                (paddingLeft + currentWidth).toFloat()
            ] = (paddingTop + height + halfDiff).toFloat()
        }
        setUpGridPoints()
        listener?.onCropRectUpdated(cropViewRect)
    }

    private fun setUpGridPoints() {
        val gridPoints = FloatArray((GRID_ROWS_COUNT) * 4 + (GRID_ROWS_COUNT) * 4)
        var index = 0
        for (i in 0 until GRID_ROWS_COUNT) {
            gridPoints[index++] = cropViewRect.left
            gridPoints[index++] = (cropViewRect.height() * ((i.toFloat() + 1.0f) / (GRID_ROWS_COUNT + 1).toFloat())) + cropViewRect.top
            gridPoints[index++] = cropViewRect.right
            gridPoints[index++] = (cropViewRect.height() * ((i.toFloat() + 1.0f) / (GRID_ROWS_COUNT + 1).toFloat())) + cropViewRect.top
        }
        for (i in 0 until GRID_COLUMNS_COUNT) {
            gridPoints[index++] = (cropViewRect.width() * ((i.toFloat() + 1.0f) / (GRID_ROWS_COUNT + 1).toFloat())) + cropViewRect.left
            gridPoints[index++] = cropViewRect.top
            gridPoints[index++] = (cropViewRect.width() * ((i.toFloat() + 1.0f) / (GRID_ROWS_COUNT + 1).toFloat())) + cropViewRect.left
            gridPoints[index++] = cropViewRect.bottom
        }
        this.gridPoints = gridPoints
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

    private fun drawDimmedLayer(canvas: Canvas) {
        canvas.save()
        canvas.clipRect(cropViewRect, Region.Op.DIFFERENCE)
        canvas.drawColor(dimColor)
        canvas.restore()
    }

    private fun drawCropGrid(canvas: Canvas) {
        gridPoints?.let { canvas.drawLines(it, gridPaint) }
        canvas.drawRect(cropViewRect, framePaint)
    }

    interface Listener {
        fun onCropRectUpdated(cropRect: RectF)
    }

    companion object {
        private const val GRID_ROWS_COUNT = 2
        private const val GRID_COLUMNS_COUNT = 2
    }

}