package com.example.ch2_project

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Segment(val value: Float, val color: Int)

    private val defaultSegments = listOf(
        Segment(41f, Color.parseColor("#758BFD")),
        Segment(25f, Color.parseColor("#6FD195")),
        Segment(20f, Color.parseColor("#FFAE4C")),
        Segment(11f, Color.parseColor("#07DBFA"))
    )

    private val defaultLabels = listOf("41%", "25%", "20%", "11%")

    private var segments: List<Segment> = defaultSegments
    private var isCustom = false

    fun setSegments(newSegments: List<Segment>) {
        segments = newSegments
        isCustom = true
        invalidate()
    }

    fun resetToDefault() {
        segments = defaultSegments
        isCustom = false
        invalidate()
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 28f
    }
    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val total = segments.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0f) return

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) * 0.9f

        oval.set(cx - radius, cy - radius, cx + radius, cy + radius)

        paint.style = Paint.Style.FILL
        var startAngle = -90f

        segments.forEachIndexed { i, segment ->
            paint.color = segment.color
            val sweep = segment.value / total * 360f
            canvas.drawArc(oval, startAngle, sweep, true, paint)

            if (!isCustom) {
                val label = if (i < defaultLabels.size) defaultLabels[i] else ""
                val midAngle = Math.toRadians((startAngle + sweep / 2).toDouble())
                val tx = (cx + radius * 0.6f * Math.cos(midAngle)).toFloat()
                val ty = (cy + radius * 0.6f * Math.sin(midAngle)).toFloat() + textPaint.textSize / 3
                canvas.drawText(label, tx, ty, textPaint)
            } else {
                val percent = (segment.value / total * 100).toInt()
                val label = "$percent%"
                val midAngle = Math.toRadians((startAngle + sweep / 2).toDouble())
                val tx = (cx + radius * 0.6f * Math.cos(midAngle)).toFloat()
                val ty = (cy + radius * 0.6f * Math.sin(midAngle)).toFloat() + textPaint.textSize / 3
                canvas.drawText(label, tx, ty, textPaint)
            }

            startAngle += sweep
        }
    }
}