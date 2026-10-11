package com.example.ch2_project

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

class CustomCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnDateSelectedListener {
        fun onDateSelected(year: Int, month: Int, day: Int)
    }

    var onDateSelectedListener: OnDateSelectedListener? = null

    private val calendar = Calendar.getInstance()
    private var selectedYear = calendar.get(Calendar.YEAR)
    private var selectedMonth = calendar.get(Calendar.MONTH)
    private var selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
    private var currentYear = calendar.get(Calendar.YEAR)
    private var currentMonth = calendar.get(Calendar.MONTH)

    private val planRanges = mutableListOf<Pair<String, String>>()
    private val scheduleMap = mutableMapOf<String, MutableList<String>>()
    private val scheduleColorMap = mutableMapOf<String, Int>()
    private val scheduleColors = listOf(
        0xFF87C5FB.toInt(),
        0xFF87C5FB.toInt(),
        0xFF87C5FB.toInt(),
        0xFF87C5FB.toInt(),
        0xFF87C5FB.toInt(),
        0xFF87C5FB.toInt()
    )

    private val dayOfWeekLabels = listOf("S", "M", "T", "W", "T", "F", "S")

    private val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 42f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 48f
        textAlign = Paint.Align.CENTER
    }
    private val dayOfWeekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }
    private val dayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }
    private val selectedDayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#625B71")
        style = Paint.Style.FILL
    }
    private val selectedDayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }
    private val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7D625B71")
        style = Paint.Style.FILL
    }
    private val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#625B71")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val scheduleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }
    private val scheduleBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var cellWidth = 0f
    private var cellHeight = 0f
    private var headerHeight = 0f
    private var dayOfWeekHeight = 0f
    private val barHeight = 24f
    private val barMargin = 4f
    private val extraHeightPerSchedule = barHeight + barMargin

    private fun getScheduleColor(title: String): Int {
        if (!scheduleColorMap.containsKey(title)) {
            val color = scheduleColors[scheduleColorMap.size % scheduleColors.size]
            scheduleColorMap[title] = color
        }
        return scheduleColorMap[title]!!
    }

    fun setPlanRanges(ranges: List<Pair<String, String>>) {
        planRanges.clear()
        planRanges.addAll(ranges)
        requestLayout()
        invalidate()
    }

    fun setSchedules(map: Map<String, List<String>>) {
        scheduleMap.clear()
        scheduleColorMap.clear()
        map.forEach { (k, v) -> scheduleMap[k] = v.toMutableList() }
        requestLayout()
        invalidate()
    }

    fun setDate(year: Int, month: Int, day: Int) {
        selectedYear = year
        selectedMonth = month
        selectedDay = day
        currentYear = year
        currentMonth = month
        requestLayout()
        invalidate()
    }

    private fun getMonthTitle(): String {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        return sdf.format(cal.time)
    }

    private fun getDaysInMonth(): Int {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun getFirstDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        return cal.get(Calendar.DAY_OF_WEEK) - 1
    }

    private fun getDateKey(year: Int, month: Int, day: Int): String {
        return String.format("%04d-%02d-%02d", year, month + 1, day)
    }

    private fun getSchedulesForDay(year: Int, month: Int, day: Int): List<String> {
        return scheduleMap[getDateKey(year, month, day)] ?: emptyList()
    }

    private fun getMaxSchedulesInRow(row: Int): Int {
        val firstDow = getFirstDayOfWeek()
        val daysInMonth = getDaysInMonth()
        var max = 0
        for (col in 0..6) {
            val pos = row * 7 + col
            val day = pos - firstDow + 1
            if (day in 1..daysInMonth) {
                val count = getSchedulesForDay(currentYear, currentMonth, day).size
                if (count > max) max = count
            }
        }
        return max
    }

    private fun getRowHeight(row: Int): Float {
        val maxSchedules = getMaxSchedulesInRow(row)
        return cellHeight + maxSchedules * extraHeightPerSchedule
    }

    private fun getRowTop(row: Int): Float {
        var top = headerHeight + dayOfWeekHeight
        for (r in 0 until row) {
            top += getRowHeight(r)
        }
        return top
    }

    private fun isInRange(year: Int, month: Int, day: Int): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
        val date = sdf.parse(dateStr) ?: return false
        for ((startStr, endStr) in planRanges) {
            val start = sdf.parse(startStr) ?: continue
            val end = sdf.parse(endStr) ?: continue
            if (!date.before(start) && !date.after(end)) return true
        }
        return false
    }

    private fun isRangeStart(year: Int, month: Int, day: Int): Boolean {
        val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
        return planRanges.any { it.first == dateStr }
    }

    private fun isRangeEnd(year: Int, month: Int, day: Int): Boolean {
        val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
        return planRanges.any { it.second == dateStr }
    }

    private fun isToday(year: Int, month: Int, day: Int): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == year &&
                today.get(Calendar.MONTH) == month &&
                today.get(Calendar.DAY_OF_MONTH) == day
    }

    private fun hasScheduleOnNextDay(day: Int, title: String): Boolean {
        val daysInMonth = getDaysInMonth()
        if (day >= daysInMonth) return false
        return getSchedulesForDay(currentYear, currentMonth, day + 1).contains(title)
    }

    private fun hasScheduleOnPrevDay(day: Int, title: String): Boolean {
        if (day <= 1) return false
        return getSchedulesForDay(currentYear, currentMonth, day - 1).contains(title)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        cellWidth = width / 7f
        headerHeight = 120f
        dayOfWeekHeight = 80f
        cellHeight = 100f

        var totalHeight = headerHeight + dayOfWeekHeight
        val rows = getRowCount()
        for (r in 0 until rows) {
            totalHeight += getRowHeight(r)
        }
        setMeasuredDimension(width, totalHeight.toInt())
    }

    private fun getRowCount(): Int {
        val firstDow = getFirstDayOfWeek()
        val daysInMonth = getDaysInMonth()
        return Math.ceil((firstDow + daysInMonth) / 7.0).toInt()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellWidth = w / 7f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val headerY = headerHeight / 2f + headerTextPaint.textSize / 3f
        canvas.drawText(getMonthTitle(), width / 2f, headerY, headerTextPaint)
        canvas.drawText("‹", cellWidth * 0.5f, headerY, arrowPaint)
        canvas.drawText("›", width - cellWidth * 0.5f, headerY, arrowPaint)

        val dowY = headerHeight + dayOfWeekHeight / 2f + dayOfWeekPaint.textSize / 3f
        dayOfWeekLabels.forEachIndexed { i, label ->
            canvas.drawText(label, cellWidth * i + cellWidth / 2f, dowY, dayOfWeekPaint)
        }

        val daysInMonth = getDaysInMonth()
        val firstDow = getFirstDayOfWeek()

        // 1패스: 장기계획 범위 배경
        for (day in 1..daysInMonth) {
            val pos = day + firstDow - 1
            val col = pos % 7
            val row = pos / 7

            val rowTop = getRowTop(row)
            val cx = cellWidth * col + cellWidth / 2f
            val cy = rowTop + cellHeight / 2f
            val radius = minOf(cellWidth, cellHeight) * 0.42f

            val inRange = isInRange(currentYear, currentMonth, day)
            val isStart = isRangeStart(currentYear, currentMonth, day)
            val isEnd = isRangeEnd(currentYear, currentMonth, day)

            if (inRange) {
                val rangeTop = cy - radius
                val rangeBottom = cy + radius
                when {
                    isStart && isEnd -> canvas.drawCircle(cx, cy, radius, rangePaint)
                    isStart -> {
                        canvas.drawRect(cx, rangeTop, cellWidth * (col + 1), rangeBottom, rangePaint)
                        canvas.drawCircle(cx, cy, radius, rangePaint)
                    }
                    isEnd -> {
                        canvas.drawRect(cellWidth * col, rangeTop, cx, rangeBottom, rangePaint)
                        canvas.drawCircle(cx, cy, radius, rangePaint)
                    }
                    else -> canvas.drawRect(cellWidth * col, rangeTop, cellWidth * (col + 1), rangeBottom, rangePaint)
                }
            }
        }

        // 2패스: 선택 원 + 오늘 테두리 + 날짜 숫자
        for (day in 1..daysInMonth) {
            val pos = day + firstDow - 1
            val col = pos % 7
            val row = pos / 7

            val rowTop = getRowTop(row)
            val cx = cellWidth * col + cellWidth / 2f
            val cy = rowTop + cellHeight / 2f
            val radius = minOf(cellWidth, cellHeight) * 0.42f

            val isSelected = (currentYear == selectedYear && currentMonth == selectedMonth && day == selectedDay)
            val isTodayDay = isToday(currentYear, currentMonth, day)

            if (isSelected) canvas.drawCircle(cx, cy, radius, selectedDayPaint)
            if (isTodayDay && !isSelected) canvas.drawCircle(cx, cy, radius, todayPaint)

            val textY = cy + dayTextPaint.textSize / 3f
            val paint = if (isSelected) selectedDayTextPaint else dayTextPaint
            canvas.drawText(day.toString(), cx, textY, paint)
        }

        // 3패스: 일정 바 (숫자 행 아래에 그리기)
        for (day in 1..daysInMonth) {
            val pos = day + firstDow - 1
            val col = pos % 7
            val row = pos / 7

            val rowTop = getRowTop(row)
            val schedules = getSchedulesForDay(currentYear, currentMonth, day)

            schedules.forEachIndexed { idx, title ->
                val barTop = rowTop + cellHeight + idx * extraHeightPerSchedule + barMargin
                val barBottom = barTop + barHeight
                val cornerRadius = barHeight / 2f

                val hasPrev = hasScheduleOnPrevDay(day, title) && col != 0
                val hasNext = hasScheduleOnNextDay(day, title) && col != 6

                val rectLeft = if (hasPrev) cellWidth * col else cellWidth * col + 4f
                val rectRight = if (hasNext) cellWidth * (col + 1) else cellWidth * (col + 1) - 4f

                scheduleBarPaint.color = getScheduleColor(title)

                when {
                    hasPrev && hasNext -> canvas.drawRect(RectF(rectLeft, barTop, rectRight, barBottom), scheduleBarPaint)
                    hasPrev && !hasNext -> {
                        canvas.drawRect(RectF(rectLeft, barTop, rectRight - cornerRadius, barBottom), scheduleBarPaint)
                        canvas.drawRoundRect(RectF(rectRight - cornerRadius * 2, barTop, rectRight, barBottom), cornerRadius, cornerRadius, scheduleBarPaint)
                    }
                    !hasPrev && hasNext -> {
                        canvas.drawRect(RectF(rectLeft + cornerRadius, barTop, rectRight, barBottom), scheduleBarPaint)
                        canvas.drawRoundRect(RectF(rectLeft, barTop, rectLeft + cornerRadius * 2, barBottom), cornerRadius, cornerRadius, scheduleBarPaint)
                    }
                    else -> canvas.drawRoundRect(RectF(rectLeft, barTop, rectRight, barBottom), cornerRadius, cornerRadius, scheduleBarPaint)
                }

                // 텍스트는 시작점에만 표시
                if (!hasPrev) {
                    val textY = barTop + barHeight / 2f + scheduleTextPaint.textSize / 3f

                    // 이어지는 바 전체 너비로 텍스트 영역 계산
                    var textRectRight = rectRight
                    var nextDay = day
                    while (hasScheduleOnNextDay(nextDay, title) && (nextDay + firstDow - 1) % 7 != 6) {
                        nextDay++
                        val nextCol = (nextDay + firstDow - 1) % 7
                        textRectRight = cellWidth * (nextCol + 1) - 4f
                    }

                    val textAreaWidth = textRectRight - rectLeft - 8f
                    var displayTitle = title

                    if (scheduleTextPaint.measureText(displayTitle) > textAreaWidth) {
                        scheduleTextPaint.textAlign = Paint.Align.LEFT
                        while (scheduleTextPaint.measureText(displayTitle) > textAreaWidth && displayTitle.isNotEmpty()) {
                            displayTitle = displayTitle.dropLast(1)
                        }
                        if (displayTitle.length < title.length) displayTitle = displayTitle.dropLast(1) + "…"
                        canvas.drawText(displayTitle, rectLeft + 4f, textY, scheduleTextPaint)
                        scheduleTextPaint.textAlign = Paint.Align.CENTER
                    } else {
                        canvas.drawText(displayTitle, (rectLeft + textRectRight) / 2f, textY, scheduleTextPaint)
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y

            if (y < headerHeight) {
                if (x < cellWidth) {
                    if (currentMonth == 0) { currentMonth = 11; currentYear-- }
                    else currentMonth--
                    requestLayout(); invalidate()
                    return true
                } else if (x > width - cellWidth) {
                    if (currentMonth == 11) { currentMonth = 0; currentYear++ }
                    else currentMonth++
                    requestLayout(); invalidate()
                    return true
                }
            }

            if (y > headerHeight + dayOfWeekHeight) {
                val firstDow = getFirstDayOfWeek()
                val daysInMonth = getDaysInMonth()
                val col = (x / cellWidth).toInt()
                val rows = getRowCount()

                for (row in 0 until rows) {
                    val rowTop = getRowTop(row)
                    val rowBottom = rowTop + getRowHeight(row)
                    if (y in rowTop..rowBottom) {
                        val pos = row * 7 + col
                        val day = pos - firstDow + 1
                        if (day in 1..daysInMonth) {
                            selectedYear = currentYear
                            selectedMonth = currentMonth
                            selectedDay = day
                            invalidate()
                            onDateSelectedListener?.onDateSelected(currentYear, currentMonth, day)
                        }
                        break
                    }
                }
            }
        }
        return true
    }
}