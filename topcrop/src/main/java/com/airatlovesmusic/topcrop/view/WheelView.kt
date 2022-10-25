package com.airatlovesmusic.topcrop.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.airatlovesmusic.topcrop.R

class WheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var listener: Listener? = null

    private val canvasClipBounds = Rect()
    private var lastTouchedPosition = 0f
    private var scrollStarted = false
    private var totalScrollDistance = 0f

    private val linesWidth: Float by lazy { context.resources.displayMetrics.density * 2 }
    private val linesHeight: Float by lazy { context.resources.displayMetrics.density * 20 }
    private val linesMargin: Float by lazy { context.resources.displayMetrics.density * 10 }

    private val progressLinePaint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = linesWidth
            color = ContextCompat.getColor(context, R.color.colorOnBackground)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> lastTouchedPosition = event.x
            MotionEvent.ACTION_UP -> {
                scrollStarted = false
                listener?.onScrollEnd()
            }
            MotionEvent.ACTION_MOVE -> {
                val distance = event.x - lastTouchedPosition
                if (distance != 0f) {
                    if (!scrollStarted) { scrollStarted = true }
                    onScrollEvent(event, distance)
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.getClipBounds(canvasClipBounds)
        val linesCount = (canvasClipBounds.width() / (linesWidth + linesMargin)).toInt()
        val deltaX = totalScrollDistance % (linesMargin + linesWidth)
        for (i in 0 until linesCount) {
            if (i < linesCount / 3) progressLinePaint.alpha = (255 * (i / (linesCount / 3).toFloat())).toInt()
            else if (i > linesCount * 2 / 3) progressLinePaint.alpha = (255 * ((linesCount - i) / (linesCount / 3).toFloat())).toInt()
            else progressLinePaint.alpha = 255
            canvas.drawLine(
                -deltaX + canvasClipBounds.left + i * (linesWidth + linesMargin),
                canvasClipBounds.centerY() - linesHeight / 4.0f,
                -deltaX + canvasClipBounds.left + i * (linesWidth + linesMargin),
                canvasClipBounds.centerY() + linesHeight / 4.0f, progressLinePaint
            )
        }
    }

    private fun onScrollEvent(event: MotionEvent, distance: Float) {
        totalScrollDistance -= distance
        postInvalidate()
        lastTouchedPosition = event.x
        listener?.onScroll(-distance, totalScrollDistance)
    }

    interface Listener {
        fun onScroll(delta: Float, totalDistance: Float)
        fun onScrollEnd()
    }
}