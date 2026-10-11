package com.example.ch2_project

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import kotlin.math.max

class StudyAnalysisActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private lateinit var totalStudyTime: TextView
    private lateinit var avgStudyTime: TextView

    private lateinit var donutChart: DonutChartView
    private lateinit var weeklyChart: LinearLayout
    private lateinit var weeklyBarContainer: LinearLayout
    private lateinit var monthlyChart: LinearLayout
    private lateinit var monthlyBarContainer: LinearLayout

    private lateinit var pieChart: PieChartView
    private lateinit var todayLegendContainer: LinearLayout
    private lateinit var defaultPieSection: LinearLayout
    private lateinit var selectedDatePieSection: LinearLayout
    private lateinit var selectedDateTitle: TextView
    private lateinit var selectedDatePieChart: PieChartView
    private lateinit var selectedDateLegend: LinearLayout
    private lateinit var btnSelectDate: TextView
    private lateinit var btnBackToToday: TextView

    private lateinit var defaultMoodSection: LinearLayout
    private lateinit var selectedDateMoodSection: LinearLayout
    private lateinit var selectedDateMoodTitle: TextView
    private lateinit var selectedMoodBars: LinearLayout
    private lateinit var selectedMoodEmojis: LinearLayout
    private lateinit var btnSelectDateMood: TextView
    private lateinit var btnBackToTodayMood: TextView

    private var displayedMoodYear: Int = 0
    private var displayedMoodMonth: Int = 0

    private val client = OkHttpClient()
    private val BASE_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/api"

    private val pieColors = listOf(
        0xFF758BFD.toInt(),
        0xFF6FD195.toInt(),
        0xFFFFAE4C.toInt(),
        0xFF07DBFA.toInt(),
        0xFFFF6B6B.toInt(),
        0xFFAB47BC.toInt()
    )

    private val moodColors = mapOf(
        1 to 0xCCE9CE1A.toInt(),
        2 to 0xCC51CC25.toInt(),
        3 to 0xCCFF9900.toInt(),
        4 to 0xCC4F5050.toInt(),
        5 to 0xCC0A89FF.toInt()
    )

    private val moodEmojis = mapOf(
        1 to "😊",
        2 to "😐",
        3 to "😢",
        4 to "😡",
        5 to "😴"
    )

    data class SubjectPieItem(
        val subject: String,
        val durationSeconds: Int,
        val percent: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study_analysis)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        totalStudyTime = findViewById(R.id.totalStudyTime)
        avgStudyTime = findViewById(R.id.avgStudyTime)

        donutChart = findViewById(R.id.donutChart)
        weeklyChart = findViewById(R.id.weeklyChart)
        weeklyBarContainer = findViewById(R.id.weeklyBarContainer)
        monthlyChart = findViewById(R.id.monthlyChart)
        monthlyBarContainer = findViewById(R.id.monthlyBarContainer)

        pieChart = findViewById(R.id.pieChart)
        todayLegendContainer = findViewById(R.id.todayLegendContainer)
        defaultPieSection = findViewById(R.id.defaultPieSection)
        selectedDatePieSection = findViewById(R.id.selectedDatePieSection)
        selectedDateTitle = findViewById(R.id.selectedDateTitle)
        selectedDatePieChart = findViewById(R.id.selectedDatePieChart)
        selectedDateLegend = findViewById(R.id.selectedDateLegend)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnBackToToday = findViewById(R.id.btnBackToToday)

        defaultMoodSection = findViewById(R.id.defaultMoodSection)
        selectedDateMoodSection = findViewById(R.id.selectedDateMoodSection)
        selectedDateMoodTitle = findViewById(R.id.selectedDateMoodTitle)
        selectedMoodBars = findViewById(R.id.selectedMoodBars)
        selectedMoodEmojis = findViewById(R.id.selectedMoodEmojis)
        btnSelectDateMood = findViewById(R.id.btnSelectDateMood)
        btnBackToTodayMood = findViewById(R.id.btnBackToTodayMood)

        drawWeeklyBars(List(7) { 0 })
        drawMonthlyBars(List(12) { 0 })

        setupCurrentMonthMoodGraph()

        fetchStudySummary()
        fetchDailyDonutChart()
        fetchTodaySubjectPieChart()
        fetchMoodByMonth(displayedMoodYear, displayedMoodMonth)

        findViewById<LinearLayout>(R.id.backButton).setOnClickListener {
            finish()
        }

        findViewById<LinearLayout>(R.id.hamburgerMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_ai -> startActivity(Intent(this, AiRecommendationActivity::class.java))
                R.id.menu_timer -> startActivity(Intent(this, TimerActivity::class.java))
                R.id.menu_today_study -> startActivity(Intent(this, TodayStudyActivity::class.java))
                R.id.menu_todo_list -> startActivity(Intent(this, TodoListActivity::class.java))
                R.id.menu_analysis -> drawerLayout.closeDrawer(GravityCompat.END)
                R.id.menu_memo -> startActivity(Intent(this, MemoActivity::class.java))
                R.id.menu_account -> startActivity(Intent(this, MyAccountActivity::class.java))
            }

            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        val btnDaily = findViewById<TextView>(R.id.btnDaily)
        val btnWeekly = findViewById<TextView>(R.id.btnWeekly)
        val btnMonthly = findViewById<TextView>(R.id.btnMonthly)

        fun selectChart(selected: String) {
            btnDaily.setBackgroundResource(
                if (selected == "daily") R.drawable.blue_tag_background_selected
                else R.drawable.blue_tag_background
            )

            btnWeekly.setBackgroundResource(
                if (selected == "weekly") R.drawable.blue_tag_background_selected
                else R.drawable.blue_tag_background
            )

            btnMonthly.setBackgroundResource(
                if (selected == "monthly") R.drawable.blue_tag_background_selected
                else R.drawable.blue_tag_background
            )

            donutChart.visibility = if (selected == "daily") View.VISIBLE else View.GONE
            weeklyChart.visibility = if (selected == "weekly") View.VISIBLE else View.GONE
            monthlyChart.visibility = if (selected == "monthly") View.VISIBLE else View.GONE
        }

        btnDaily.setOnClickListener {
            selectChart("daily")
            fetchDailyDonutChart()
        }

        btnWeekly.setOnClickListener {
            selectChart("weekly")
            fetchWeeklyChart()
        }

        btnMonthly.setOnClickListener {
            selectChart("monthly")
            fetchMonthlyChart()
        }

        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    val displayDate = "${month + 1}월 ${dayOfMonth}일"

                    selectedDateTitle.text = "과목별 학습시간 : $displayDate"
                    defaultPieSection.visibility = View.GONE
                    selectedDatePieSection.visibility = View.VISIBLE

                    fetchStudyRecordsByDate(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnBackToToday.setOnClickListener {
            selectedDatePieSection.visibility = View.GONE
            defaultPieSection.visibility = View.VISIBLE
            fetchTodaySubjectPieChart()
        }

        btnSelectDateMood.setOnClickListener {
            showMonthPickerDialog()
        }

        btnBackToTodayMood.setOnClickListener {
            setupCurrentMonthMoodGraph()
            fetchMoodByMonth(displayedMoodYear, displayedMoodMonth)
        }

        findViewById<LinearLayout>(R.id.nav_ai).setOnClickListener {
            startActivity(Intent(this, AiRecommendationActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_schedule).setOnClickListener {
            startActivity(Intent(this, TodayStudyActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_timer).setOnClickListener {
            startActivity(Intent(this, TimerActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_todo).setOnClickListener {
            startActivity(Intent(this, TodoListActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_analysis).setOnClickListener {
            // 현재 화면
        }

        findViewById<LinearLayout>(R.id.nav_memo).setOnClickListener {
            startActivity(Intent(this, MemoActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        if (::totalStudyTime.isInitialized && ::avgStudyTime.isInitialized) {
            fetchStudySummary()
        }

        if (::donutChart.isInitialized) {
            fetchDailyDonutChart()
        }

        if (::pieChart.isInitialized && ::todayLegendContainer.isInitialized) {
            fetchTodaySubjectPieChart()
        }

        if (::selectedMoodBars.isInitialized && ::selectedMoodEmojis.isInitialized) {
            if (displayedMoodYear == 0 || displayedMoodMonth == 0) {
                setupCurrentMonthMoodGraph()
            }

            fetchMoodByMonth(displayedMoodYear, displayedMoodMonth)
        }
    }

    private fun getAuthToken(): String? {
        return getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("auth_token", null)
    }

    private fun setupCurrentMonthMoodGraph() {
        val calendar = Calendar.getInstance()

        displayedMoodYear = calendar.get(Calendar.YEAR)
        displayedMoodMonth = calendar.get(Calendar.MONTH) + 1

        defaultMoodSection.visibility = View.GONE
        selectedDateMoodSection.visibility = View.VISIBLE
        btnBackToTodayMood.visibility = View.GONE

        selectedDateMoodTitle.text = "월별 감정 그래프 : ${displayedMoodYear}년 ${displayedMoodMonth}월"
    }

    private fun showMonthPickerDialog() {
        val calendar = Calendar.getInstance()

        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        val yearPicker = NumberPicker(this).apply {
            minValue = 2020
            maxValue = 2030
            value = if (displayedMoodYear == 0) calendar.get(Calendar.YEAR) else displayedMoodYear
        }

        val yearLabel = TextView(this).apply {
            text = " 년  "
            textSize = 18f
            setTextColor(Color.BLACK)
        }

        val monthPicker = NumberPicker(this).apply {
            minValue = 1
            maxValue = 12
            value = if (displayedMoodMonth == 0) calendar.get(Calendar.MONTH) + 1 else displayedMoodMonth
        }

        val monthLabel = TextView(this).apply {
            text = " 월"
            textSize = 18f
            setTextColor(Color.BLACK)
        }

        dialogView.addView(yearPicker)
        dialogView.addView(yearLabel)
        dialogView.addView(monthPicker)
        dialogView.addView(monthLabel)

        AlertDialog.Builder(this)
            .setTitle("월 선택")
            .setView(dialogView)
            .setPositiveButton("확인") { _, _ ->
                displayedMoodYear = yearPicker.value
                displayedMoodMonth = monthPicker.value

                selectedDateMoodTitle.text = "월별 감정 그래프 : ${displayedMoodYear}년 ${displayedMoodMonth}월"
                defaultMoodSection.visibility = View.GONE
                selectedDateMoodSection.visibility = View.VISIBLE

                val now = Calendar.getInstance()
                val currentYear = now.get(Calendar.YEAR)
                val currentMonth = now.get(Calendar.MONTH) + 1

                btnBackToTodayMood.visibility =
                    if (displayedMoodYear == currentYear && displayedMoodMonth == currentMonth) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }

                fetchMoodByMonth(displayedMoodYear, displayedMoodMonth)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun fetchStudySummary() {
        val token = getAuthToken()

        if (token == null) {
            totalStudyTime.text = "0시간"
            avgStudyTime.text = "0시간"
            return
        }

        val request = Request.Builder()
            .url("$BASE_URL/study-summary")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    totalStudyTime.text = "0시간"
                    avgStudyTime.text = "0시간"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        try {
                            val jsonObject = JSONObject(body)
                            val todayTotalDuration = jsonObject.optInt("today_total_duration", 0)
                            val monthAvgDuration = jsonObject.optInt("month_avg_duration", 0)

                            totalStudyTime.text = formatDurationSeconds(todayTotalDuration)
                            avgStudyTime.text = formatDurationSeconds(monthAvgDuration)
                        } catch (e: Exception) {
                            totalStudyTime.text = "0시간"
                            avgStudyTime.text = "0시간"
                        }
                    } else {
                        totalStudyTime.text = "0시간"
                        avgStudyTime.text = "0시간"
                    }
                }
            }
        })
    }

    private fun fetchDailyDonutChart() {
        val token = getAuthToken()

        if (token == null) {
            donutChart.setChartData(0f, "00:00")
            return
        }

        val request = Request.Builder()
            .url("$BASE_URL/study/daily-donut")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    donutChart.setChartData(0f, "00:00")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        try {
                            val jsonObject = JSONObject(body)
                            val todayStudySeconds = jsonObject.optInt("today_study_seconds", 0)
                            val progress = jsonObject.optDouble("progress", 0.0).toFloat()

                            donutChart.setChartData(progress, formatDurationClock(todayStudySeconds))
                        } catch (e: Exception) {
                            donutChart.setChartData(0f, "00:00")
                        }
                    } else {
                        donutChart.setChartData(0f, "00:00")
                    }
                }
            }
        })
    }

    private fun fetchWeeklyChart() {
        val token = getAuthToken()

        if (token == null) {
            drawWeeklyBars(List(7) { 0 })
            return
        }

        val request = Request.Builder()
            .url("$BASE_URL/study/weekly-chart")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    drawWeeklyBars(List(7) { 0 })
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        try {
                            val jsonObject = JSONObject(body)
                            val array = jsonObject.optJSONArray("weekly_seconds")
                            val values = mutableListOf<Int>()

                            for (i in 0 until 7) {
                                values.add(array?.optInt(i, 0) ?: 0)
                            }

                            drawWeeklyBars(values)
                        } catch (e: Exception) {
                            drawWeeklyBars(List(7) { 0 })
                        }
                    } else {
                        drawWeeklyBars(List(7) { 0 })
                    }
                }
            }
        })
    }

    private fun fetchMonthlyChart() {
        val token = getAuthToken()

        if (token == null) {
            drawMonthlyBars(List(12) { 0 })
            return
        }

        val request = Request.Builder()
            .url("$BASE_URL/study/monthly-chart")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    drawMonthlyBars(List(12) { 0 })
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        try {
                            val jsonObject = JSONObject(body)
                            val array = jsonObject.optJSONArray("monthly_seconds")
                            val values = mutableListOf<Int>()

                            for (i in 0 until 12) {
                                values.add(array?.optInt(i, 0) ?: 0)
                            }

                            drawMonthlyBars(values)
                        } catch (e: Exception) {
                            drawMonthlyBars(List(12) { 0 })
                        }
                    } else {
                        drawMonthlyBars(List(12) { 0 })
                    }
                }
            }
        })
    }

    private fun fetchTodaySubjectPieChart() {
        val token = getAuthToken()

        if (token == null) {
            showEmptyPieChart(pieChart, todayLegendContainer)
            return
        }

        val request = Request.Builder()
            .url("$BASE_URL/study/subject-summary")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    showEmptyPieChart(pieChart, todayLegendContainer)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        try {
                            val items = parseSubjectPieItems(body)

                            if (items.isEmpty()) {
                                showEmptyPieChart(pieChart, todayLegendContainer)
                            } else {
                                showSubjectPieChart(pieChart, todayLegendContainer, items)
                            }
                        } catch (e: Exception) {
                            showEmptyPieChart(pieChart, todayLegendContainer)
                        }
                    } else {
                        showEmptyPieChart(pieChart, todayLegendContainer)
                    }
                }
            }
        })
    }

    private fun fetchStudyRecordsByDate(date: String) {
        val token = getAuthToken()

        if (token == null) {
            showEmptyPieChart(selectedDatePieChart, selectedDateLegend)
            return
        }

        val request = Request.Builder()
            .url("$BASE_URL/study/subject-summary?date=$date")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    showEmptyPieChart(selectedDatePieChart, selectedDateLegend)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        try {
                            val items = parseSubjectPieItems(body)

                            if (items.isEmpty()) {
                                showEmptyPieChart(selectedDatePieChart, selectedDateLegend)
                            } else {
                                showSubjectPieChart(selectedDatePieChart, selectedDateLegend, items)
                            }
                        } catch (e: Exception) {
                            showEmptyPieChart(selectedDatePieChart, selectedDateLegend)
                        }
                    } else {
                        showEmptyPieChart(selectedDatePieChart, selectedDateLegend)
                    }
                }
            }
        })
    }

    private fun fetchMoodByMonth(year: Int, month: Int) {
        val token = getAuthToken()

        if (token == null) {
            showEmptyMoodGraph()
            return
        }

        val yearMonth = String.format("%04d-%02d", year, month)

        val request = Request.Builder()
            .url("$BASE_URL/diary/mood-summary?month=$yearMonth")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    showEmptyMoodGraph()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        try {
                            val jsonObject = JSONObject(body)
                            val itemsArray = jsonObject.optJSONArray("items") ?: JSONArray()
                            val moodCount = mutableMapOf<Int, Int>()

                            for (i in 0 until itemsArray.length()) {
                                val item = itemsArray.getJSONObject(i)
                                val moodId = item.optInt("mood_id", 0)
                                val count = item.optInt("count", 0)

                                if (moodId in 1..5) {
                                    moodCount[moodId] = count
                                }
                            }

                            showSelectedMoodGraph(moodCount)
                        } catch (e: Exception) {
                            showEmptyMoodGraph()
                        }
                    } else {
                        showEmptyMoodGraph()
                    }
                }
            }
        })
    }

    private fun parseSubjectPieItems(body: String): List<SubjectPieItem> {
        val jsonObject = JSONObject(body)
        val itemsArray = jsonObject.optJSONArray("items") ?: JSONArray()
        val result = mutableListOf<SubjectPieItem>()

        for (i in 0 until itemsArray.length()) {
            val item = itemsArray.getJSONObject(i)

            result.add(
                SubjectPieItem(
                    subject = item.optString("subject", "기타"),
                    durationSeconds = item.optInt("duration_seconds", 0),
                    percent = item.optInt("percent", 0)
                )
            )
        }

        return result
    }

    private fun showSubjectPieChart(
        chartView: PieChartView,
        legendContainer: LinearLayout,
        items: List<SubjectPieItem>
    ) {
        val segments = mutableListOf<PieChartView.Segment>()

        legendContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            val color = pieColors[index % pieColors.size]

            segments.add(
                PieChartView.Segment(
                    item.percent.toFloat(),
                    color
                )
            )

            val legendItem = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 6.dpToPx())
                }
            }

            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    8.dpToPx(),
                    8.dpToPx()
                ).apply {
                    setMargins(0, 0, 6.dpToPx(), 0)
                }

                setBackgroundColor(color)
            }

            val label = TextView(this).apply {
                text = "${item.subject} ${item.percent}%"
                textSize = 12f
                setTextColor(Color.BLACK)
            }

            legendItem.addView(dot)
            legendItem.addView(label)
            legendContainer.addView(legendItem)
        }

        chartView.setSegments(segments)
    }

    private fun showEmptyPieChart(
        chartView: PieChartView,
        legendContainer: LinearLayout
    ) {
        legendContainer.removeAllViews()

        val emptyLabel = TextView(this).apply {
            text = "학습 기록이 없습니다."
            textSize = 12f
            setTextColor(Color.GRAY)
        }

        legendContainer.addView(emptyLabel)

        chartView.setSegments(
            listOf(
                PieChartView.Segment(
                    100f,
                    Color.LTGRAY
                )
            )
        )
    }

    private fun drawWeeklyBars(weeklySeconds: List<Int>) {
        weeklyBarContainer.removeAllViews()

        val maxSeconds = max(weeklySeconds.maxOrNull() ?: 0, 1)
        val maxBarHeight = 90.dpToPx()
        val minBarHeight = 2.dpToPx()

        for (i in 0 until 7) {
            val seconds = weeklySeconds.getOrNull(i) ?: 0

            val barHeight = if (seconds <= 0) {
                minBarHeight
            } else {
                max(minBarHeight, (seconds.toFloat() / maxSeconds * maxBarHeight).toInt())
            }

            val barWrapper = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                ).apply {
                    setMargins(5.dpToPx(), 0, 5.dpToPx(), 0)
                }

                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            }

            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    16.dpToPx(),
                    barHeight
                )

                setBackgroundColor(Color.parseColor("#CC7086FD"))
            }

            barWrapper.addView(bar)
            weeklyBarContainer.addView(barWrapper)
        }
    }

    private fun drawMonthlyBars(monthlySeconds: List<Int>) {
        monthlyBarContainer.removeAllViews()

        val maxSeconds = max(monthlySeconds.maxOrNull() ?: 0, 1)
        val maxBarHeight = 90.dpToPx()
        val minBarHeight = 2.dpToPx()

        for (i in 0 until 12) {
            val seconds = monthlySeconds.getOrNull(i) ?: 0

            val barHeight = if (seconds <= 0) {
                minBarHeight
            } else {
                max(minBarHeight, (seconds.toFloat() / maxSeconds * maxBarHeight).toInt())
            }

            val monthColumn = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    28.dpToPx(),
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(3.dpToPx(), 0, 3.dpToPx(), 0)
                }

                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM
            }

            val barArea = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )

                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            }

            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    14.dpToPx(),
                    barHeight
                )

                setBackgroundColor(Color.parseColor("#CC7086FD"))
            }

            val monthLabel = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                text = "${i + 1}월"
                textSize = 8f
                setTextColor(Color.parseColor("#666666"))
                gravity = android.view.Gravity.CENTER
            }

            barArea.addView(bar)
            monthColumn.addView(barArea)
            monthColumn.addView(monthLabel)
            monthlyBarContainer.addView(monthColumn)
        }
    }

    private fun showSelectedMoodGraph(moodCount: Map<Int, Int>) {
        val maxCount = max(moodCount.values.maxOrNull() ?: 0, 1)

        selectedMoodBars.removeAllViews()
        selectedMoodEmojis.removeAllViews()

        val maxBarHeight = 90
        val minBarHeight = 2

        for (moodId in 1..5) {
            val count = moodCount[moodId] ?: 0

            val barHeight = if (count <= 0) {
                minBarHeight
            } else {
                max(minBarHeight, (count.toFloat() / maxCount * maxBarHeight).toInt())
            }

            addMoodBar(moodId, barHeight, count)
        }
    }

    private fun showEmptyMoodGraph() {
        selectedMoodBars.removeAllViews()
        selectedMoodEmojis.removeAllViews()

        for (moodId in 1..5) {
            addMoodBar(moodId, 2, 0)
        }
    }

    private fun addMoodBar(moodId: Int, barHeightDp: Int, count: Int) {
        val color = moodColors[moodId] ?: Color.GRAY
        val emoji = moodEmojis[moodId] ?: ""

        val column = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                setMargins(6.dpToPx(), 0, 6.dpToPx(), 0)
            }

            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
        }

        val countText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            text = count.toString()
            textSize = 8f
            setTextColor(Color.parseColor("#777777"))
            gravity = android.view.Gravity.CENTER
        }

        val bar = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                barHeightDp.dpToPx()
            )

            setBackgroundColor(color)
        }

        column.addView(countText)
        column.addView(bar)
        selectedMoodBars.addView(column)

        val emojiView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            text = emoji
            textSize = 14f
            gravity = android.view.Gravity.CENTER
        }

        selectedMoodEmojis.addView(emojiView)
    }

    private fun formatDurationSeconds(durationSeconds: Int): String {
        val totalMinutes = durationSeconds / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
            hours > 0 -> "${hours}시간"
            minutes > 0 -> "${minutes}분"
            else -> "0시간"
        }
    }

    private fun formatDurationClock(durationSeconds: Int): String {
        val totalMinutes = durationSeconds / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return String.format("%02d:%02d", hours, minutes)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}