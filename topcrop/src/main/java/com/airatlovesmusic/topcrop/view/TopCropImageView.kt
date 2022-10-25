package com.airatlovesmusic.topcrop.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import androidx.annotation.IntRange
import androidx.appcompat.widget.AppCompatImageView
import com.airatlovesmusic.topcrop.utils.getRectCenter
import com.airatlovesmusic.topcrop.utils.getRectCorners
import com.airatlovesmusic.topcrop.utils.getRectSidesFromCorners
import com.airatlovesmusic.topcrop.utils.trapToRect
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

open class TopCropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): AppCompatImageView(context, attrs, defStyle) {

    // Gestures
    private val scaleDetector: ScaleGestureDetector by lazy { ScaleGestureDetector(context, ScaleListener()) }
    private val gestureDetector: GestureDetector by lazy { GestureDetector(context, GestureListener(), null, true) }
    private val rotationDetector: RotationDetector by lazy { RotationDetector { postRotate(it, middlePointX, middlePointY) } }

    private var middlePointX = 0f
    private var middlePointY = 0f

    private var currentAspectRatio: Float = 1f
    private var currentImageMatrix = Matrix()
    private val currentImageCorners = FloatArray(RECT_CORNER_COORDS_COUNT)
    private val currentImageCenter = FloatArray(RECT_CENTER_COORDS_COUNT)

    private val cropRectF = RectF()
    private val tmpMatrix = Matrix()

    private var bitmapLaidOut: Boolean = false
    private var currentWidth: Float = 0f
    private var currentHeight: Float = 0f
    private var initialImageCorners = FloatArray(RECT_CORNER_COORDS_COUNT)
    private var initialImageCenter = FloatArray(RECT_CENTER_COORDS_COUNT)
    var minScale = 1f
    var maxScale = 1f
    private var maxScaleMultiplier = 10.0f

    var listener: Listener? = null

    init { scaleType = ScaleType.MATRIX }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1) {
            middlePointX = (event.getX(0) + event.getX(1)) / 2
            middlePointY = (event.getY(0) + event.getY(1)) / 2
        }
        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)
        rotationDetector.onTouchEvent(event)

        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP) {
            setImageFitAspectRatio()
        }
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed || !bitmapLaidOut) {
            currentWidth = (width - paddingRight - paddingLeft).toFloat()
            currentHeight = (height - paddingBottom - paddingTop).toFloat()
            onImageLaidOut()
        }
    }

    override fun setImageMatrix(matrix: Matrix?) {
        super.setImageMatrix(matrix)
        currentImageMatrix.set(matrix)
        currentImageMatrix.mapPoints(currentImageCorners, initialImageCorners)
        currentImageMatrix.mapPoints(currentImageCenter, initialImageCenter)
    }

    fun setCropRect(cropRect: RectF) {
        currentAspectRatio = cropRect.width() / cropRect.height()
        cropRectF.set(
            cropRect.left - paddingLeft, cropRect.top - paddingTop,
            cropRect.right - paddingRight, cropRect.bottom - paddingBottom
        )
        drawable?.let { calculateImageScaleBounds(it.intrinsicWidth.toFloat(), it.intrinsicHeight.toFloat()) }
        setImageFitAspectRatio()
    }

    fun setTargetAspectRatio(aspectRatio: Float) {
        currentAspectRatio = aspectRatio
        setImageFitAspectRatio()
    }

    fun setImageFitAspectRatio() {
        if (bitmapLaidOut && !isImageWrapCropBounds(currentImageCorners)) {
            val currentX: Float = currentImageCenter[0]
            val currentY: Float = currentImageCenter[1]
            val currentScale: Float = getMatrixScale(currentImageMatrix)
            var deltaX: Float = cropRectF.centerX() - currentX
            var deltaY: Float = cropRectF.centerY() - currentY
            var deltaScale = 0f
            tmpMatrix.reset()
            tmpMatrix.setTranslate(deltaX, deltaY)

            val tempCurrentImageCorners: FloatArray = currentImageCorners.copyOf()
            tmpMatrix.mapPoints(tempCurrentImageCorners)

            val willImageWrapCropBoundsAfterTranslate: Boolean = isImageWrapCropBounds(tempCurrentImageCorners)
            if (willImageWrapCropBoundsAfterTranslate) {
                val imageIndents: FloatArray = calculateImageIndents()
                deltaX = -(imageIndents[0] + imageIndents[2])
                deltaY = -(imageIndents[1] + imageIndents[3])
            } else {
                val tempCropRect = RectF(cropRectF)
                tmpMatrix.reset()
                tmpMatrix.setRotate(getMatrixAngle(currentImageMatrix))
                tmpMatrix.mapRect(tempCropRect)
                val currentImageSides: FloatArray = getRectSidesFromCorners(currentImageCorners)
                deltaScale = (tempCropRect.width() / currentImageSides[0]).coerceAtLeast(tempCropRect.height() / currentImageSides[1])
                deltaScale = deltaScale * currentScale - currentScale
            }
            postTranslate(deltaX, deltaY)
            if (!willImageWrapCropBoundsAfterTranslate) {
                zoomInImage(currentScale + deltaScale, cropRectF.centerX(), cropRectF.centerY())
            }
        }
    }

    fun zoomImage(delta: Float) {
        postScale(getMatrixScale(matrix) + 0.1f * delta, cropRectF.centerX(), cropRectF.centerY())
    }

    private fun postTranslate(deltaX: Float, deltaY: Float) {
        if (deltaX != 0f || deltaY != 0f) {
            currentImageMatrix.postTranslate(deltaX, deltaY)
            imageMatrix = currentImageMatrix
        }
    }

    private fun zoomInImage(scale: Float, centerX: Float = cropRectF.centerX(), centerY: Float = cropRectF.centerY()) {
        if (scale <= maxScale) {
            postScale(scale / getMatrixScale(currentImageMatrix), centerX, centerY)
        }
    }

    fun postScale(
        deltaScale: Float,
        px: Float = cropRectF.centerX(),
        py: Float = cropRectF.centerY()
    ) {
        if (deltaScale != 0f) {
            currentImageMatrix.postScale(deltaScale, deltaScale, px, py)
            imageMatrix = currentImageMatrix
            listener?.onScale(getMatrixScale(currentImageMatrix))
        }
    }

    fun postRotate(
        deltaAngle: Float,
        px: Float = cropRectF.centerX(),
        py: Float = cropRectF.centerY()
    ) {
        if (deltaAngle != 0f) {
            currentImageMatrix.postRotate(deltaAngle, px, py)
            imageMatrix = currentImageMatrix
            listener?.onRotate(getMatrixAngle(currentImageMatrix))
        }
    }

    private fun onImageLaidOut() {
        val drawable = drawable ?: return
        val w = drawable.intrinsicWidth.toFloat()
        val h = drawable.intrinsicHeight.toFloat()

        val initialImageRect = RectF(0f, 0f, w, h)
        initialImageCorners = getRectCorners(initialImageRect)
        initialImageCenter = getRectCenter(initialImageRect)
        bitmapLaidOut = true

        val height = currentWidth / currentAspectRatio
        if (height > currentHeight) {
            val width = (currentHeight * currentAspectRatio).toInt()
            val halfDiff = (currentWidth - width) / 2
            cropRectF.set(halfDiff, 0f, (width + halfDiff), currentHeight)
        } else {
            val halfDiff = (currentHeight - height) / 2
            cropRectF.set(0f, halfDiff, currentWidth, (height + halfDiff))
        }
        calculateImageScaleBounds(w, h)
        setupInitialImagePosition(w, h)
        listener?.onScale(getMatrixScale(currentImageMatrix))
        listener?.onRotate(getMatrixAngle(currentImageMatrix))
    }

    private fun calculateImageScaleBounds(drawableWidth: Float, drawableHeight: Float) {
        val widthScale: Float = min(cropRectF.width() / drawableWidth, cropRectF.width() / drawableHeight)
        val heightScale: Float = min(cropRectF.height() / drawableHeight, cropRectF.height() / drawableWidth)
        minScale = min(widthScale, heightScale)
        maxScale = minScale * maxScaleMultiplier
    }

    private fun setupInitialImagePosition(drawableWidth: Float, drawableHeight: Float) {
        val cropRectWidth: Float = cropRectF.width()
        val cropRectHeight: Float = cropRectF.height()
        val widthScale: Float = cropRectF.width() / drawableWidth
        val heightScale: Float = cropRectF.height() / drawableHeight
        val initialMinScale = Math.max(widthScale, heightScale)
        val tw: Float = (cropRectWidth - drawableWidth * initialMinScale) / 2.0f + cropRectF.left
        val th: Float = (cropRectHeight - drawableHeight * initialMinScale) / 2.0f + cropRectF.top
        currentImageMatrix.reset()
        currentImageMatrix.postScale(initialMinScale, initialMinScale)
        currentImageMatrix.postTranslate(tw, th)
        imageMatrix = currentImageMatrix
    }

    private fun calculateImageIndents(): FloatArray {
        tmpMatrix.reset()
        tmpMatrix.setRotate(-getMatrixAngle(currentImageMatrix))
        val unrotatedImageCorners: FloatArray = currentImageCorners.copyOf()
        tmpMatrix.mapPoints(unrotatedImageCorners)
        val unrotatedCropBoundsCorners: FloatArray = getRectCorners(cropRectF)
        tmpMatrix.mapPoints(unrotatedCropBoundsCorners)
        val unrotatedImageRect: RectF = trapToRect(unrotatedImageCorners)
        val unrotatedCropRect: RectF = trapToRect(unrotatedCropBoundsCorners)
        val deltaLeft = unrotatedImageRect.left - unrotatedCropRect.left
        val deltaTop = unrotatedImageRect.top - unrotatedCropRect.top
        val deltaRight = unrotatedImageRect.right - unrotatedCropRect.right
        val deltaBottom = unrotatedImageRect.bottom - unrotatedCropRect.bottom
        val indents = FloatArray(4)
        indents[0] = if (deltaLeft > 0) deltaLeft else 0f
        indents[1] = if (deltaTop > 0) deltaTop else 0f
        indents[2] = if (deltaRight < 0) deltaRight else 0f
        indents[3] = if (deltaBottom < 0) deltaBottom else 0f
        tmpMatrix.reset()
        tmpMatrix.setRotate(getMatrixAngle(currentImageMatrix))
        tmpMatrix.mapPoints(indents)
        return indents
    }

    private fun isImageWrapCropBounds(imageCorners: FloatArray): Boolean {
        tmpMatrix.reset()
        tmpMatrix.setRotate(-getMatrixAngle(currentImageMatrix))
        val unrotatedImageCorners = imageCorners.copyOf()
        tmpMatrix.mapPoints(unrotatedImageCorners)
        val unrotatedCropBoundsCorners: FloatArray = getRectCorners(cropRectF)
        tmpMatrix.mapPoints(unrotatedCropBoundsCorners)
        return trapToRect(unrotatedImageCorners)
            .contains(trapToRect(unrotatedCropBoundsCorners))
    }

    private fun getMatrixAngle(matrix: Matrix): Float = -(atan2(
        getMatrixValue(matrix, Matrix.MSKEW_X).toDouble(),
        getMatrixValue(matrix, Matrix.MSCALE_X).toDouble()
    ) * (180 / Math.PI)).toFloat()

    private fun getMatrixScale(matrix: Matrix): Float = sqrt(
        getMatrixValue(matrix, Matrix.MSCALE_X).toDouble().pow(2.0)
            + getMatrixValue(matrix, Matrix.MSKEW_Y).toDouble().pow(2.0)
    ).toFloat()

    private fun getMatrixValue(
        matrix: Matrix,
        @IntRange(from = 0, to = 9) valueIndex: Int
    ): Float {
        val matrixValues = FloatArray(9)
        matrix.getValues(matrixValues)
        return matrixValues[valueIndex]
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            postScale(detector.scaleFactor, middlePointX, middlePointY)
            return true
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

    interface Listener {
        fun onRotate(angle: Float)
        fun onScale(scale: Float)
    }

    companion object {
        const val RECT_CORNER_COORDS_COUNT = 8
        const val RECT_CENTER_COORDS_COUNT = 2
    }

}