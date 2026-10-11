package com.example.ch2_project

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val oval = RectF()

    private var progress: Float = 0f
    private var centerText: String = "00:00"

    private val backgroundColor = Color.parseColor("#E0E0E0")
    private val progressColor = Color.parseColor("#758BFD")
    private val textColor = Color.BLACK

    fun setProgress(value: Float) {
        progress = min(1f, max(0f, value))
        invalidate()
    }

    fun setCenterText(text: String) {
        centerText = text
        invalidate()
    }

    fun setChartData(value: Float, text: String) {
        progress = min(1f, max(0f, value))
        centerText = text
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) * 0.85f
        val strokeWidth = radius * 0.3f

        oval.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // 회색 전체 원
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = backgroundColor

        canvas.drawArc(oval, 0f, 360f, false, paint)

        // 파란색 진행 부분
        val sweepAngle = 360f * progress

        if (sweepAngle > 0f) {
            paint.color = progressColor
            canvas.drawArc(oval, -90f, sweepAngle, false, paint)
        }

        // 가운데 텍스트
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = textColor
        paint.textSize = radius * 0.35f
        paint.textAlign = Paint.Align.CENTER

        val fontMetrics = paint.fontMetrics
        val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f

        canvas.drawText(centerText, cx, textY, paint)
    }
}