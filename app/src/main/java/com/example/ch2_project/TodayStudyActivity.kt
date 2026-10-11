package com.example.ch2_project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class TodayStudyActivity : AppCompatActivity() {

    private val BASE_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/api"
    private val client = OkHttpClient()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var diaryContentText: TextView
    private lateinit var selectedMoodText: TextView
    private lateinit var moodContainer: LinearLayout
    private lateinit var studyTaskAdapter: StudyTaskAdapter
    private lateinit var customCalendarView: CustomCalendarView

    private var currentSelectedDate = ""
    private var currentMoodId = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_study)

        drawerLayout = findViewById(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        customCalendarView = findViewById(R.id.customCalendarView)
        diaryContentText = findViewById(R.id.diaryContentText)
        selectedMoodText = findViewById(R.id.selectedMood)
        moodContainer = findViewById(R.id.moodContainer)

        val recyclerView = findViewById<RecyclerView>(R.id.studyTaskRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        studyTaskAdapter = StudyTaskAdapter(mutableListOf()) { taskId, isChecked ->
            updateTaskCheckOnServer(taskId, isChecked)
        }

        recyclerView.adapter = studyTaskAdapter

        val today = Calendar.getInstance()
        currentSelectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.time)

        customCalendarView.setDate(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        )

        updateDateLabels(currentSelectedDate)

        customCalendarView.onDateSelectedListener = object : CustomCalendarView.OnDateSelectedListener {
            override fun onDateSelected(year: Int, month: Int, day: Int) {
                currentSelectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                updateDateLabels(currentSelectedDate)
                loadData()
            }
        }

        fetchAllSchedulesForCalendar()

        diaryContentText.setOnClickListener {
            showDiaryEditDialog()
        }

        selectedMoodText.setOnClickListener {
            moodContainer.visibility =
                if (moodContainer.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        setupMoodButtons()

        findViewById<LinearLayout>(R.id.backButton).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.scheduleEditButton).setOnClickListener {
            val intent = Intent(this, ScheduleEditActivity::class.java)
            intent.putExtra("date", currentSelectedDate)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.hamburgerMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_ai -> startActivity(Intent(this, AiRecommendationActivity::class.java))
                R.id.menu_timer -> startActivity(Intent(this, TimerActivity::class.java))
                R.id.menu_today_study -> drawerLayout.closeDrawer(GravityCompat.END)
                R.id.menu_todo_list -> startActivity(Intent(this, TodoListActivity::class.java))
                R.id.menu_analysis -> startActivity(Intent(this, StudyAnalysisActivity::class.java))
                R.id.menu_memo -> startActivity(Intent(this, MemoActivity::class.java))
                R.id.menu_account -> startActivity(Intent(this, MyAccountActivity::class.java))
            }

            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        findViewById<LinearLayout>(R.id.nav_ai).setOnClickListener {
            startActivity(Intent(this, AiRecommendationActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_schedule).setOnClickListener {
            // 현재 화면
        }

        findViewById<LinearLayout>(R.id.nav_timer).setOnClickListener {
            startActivity(Intent(this, TimerActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_todo).setOnClickListener {
            startActivity(Intent(this, TodoListActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_analysis).setOnClickListener {
            startActivity(Intent(this, StudyAnalysisActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_memo).setOnClickListener {
            startActivity(Intent(this, MemoActivity::class.java))
        }

        loadData()
    }

    private fun fetchAllSchedulesForCalendar() {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("auth_token", null) ?: return

        val request = Request.Builder()
            .url("$BASE_URL/schedules")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("CALENDAR", "일정 불러오기 실패: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    try {
                        val jsonArray = JSONArray(body)

                        val scheduleMap = mutableMapOf<String, MutableList<String>>()
                        val ranges = mutableListOf<Pair<String, String>>()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)

                            val title = obj.optString("title", "")
                            val targetDate = obj.optString("target_date", "")
                            val startDate = obj.optString("start_date", "")
                            val endDate = obj.optString("end_date", "")

                            val displayText = getCalendarDisplayTextFromSchedule(obj, title)

                            if (targetDate.isNotEmpty() && displayText.isNotEmpty()) {
                                scheduleMap
                                    .getOrPut(targetDate) { mutableListOf() }
                                    .add(displayText)
                            }

                            if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                                ranges.add(Pair(startDate, endDate))
                            }
                        }

                        runOnUiThread {
                            customCalendarView.setSchedules(scheduleMap)
                            customCalendarView.setPlanRanges(ranges)
                        }

                    } catch (e: Exception) {
                        android.util.Log.e("CALENDAR", "파싱 오류: ${e.message}")
                    }
                } else {
                    android.util.Log.e("CALENDAR", "응답 실패: ${response.code}")
                }
            }
        })
    }

    private fun getCalendarDisplayTextFromSchedule(scheduleObj: JSONObject, fallbackTitle: String): String {
        val tasksArray = scheduleObj.optJSONArray("tasks")
        val subjectList = mutableListOf<String>()

        if (tasksArray != null) {
            for (i in 0 until tasksArray.length()) {
                val taskObj = tasksArray.optJSONObject(i) ?: continue

                val subject = taskObj.optString("subject", "").trim()
                val content = taskObj.optString("content", "").trim()

                when {
                    subject.isNotEmpty() && subject != "null" && subject != "기타" -> {
                        subjectList.add(subject)
                    }

                    content.isNotEmpty() && content != "null" -> {
                        val guessedSubject = extractSubjectFromContent(content)
                        if (guessedSubject.isNotEmpty()) {
                            subjectList.add(guessedSubject)
                        }
                    }
                }
            }
        }

        val distinctSubjects = subjectList
            .filter { it.isNotEmpty() }
            .distinct()

        return when {
            distinctSubjects.isNotEmpty() -> distinctSubjects.joinToString(", ")
            fallbackTitle.isNotEmpty() -> fallbackTitle
            else -> "학습"
        }
    }

    private fun extractSubjectFromContent(content: String): String {
        val trimmed = content.trim()

        if (trimmed.isEmpty()) {
            return ""
        }

        if (trimmed.contains("-")) {
            return trimmed.substringBefore("-").trim()
        }

        if (trimmed.contains(" ")) {
            return trimmed.substringBefore(" ").trim()
        }

        return trimmed
    }

    private fun updateDateLabels(date: String) {
        val dotDate = date.replace("-", ".")
        findViewById<TextView>(R.id.diaryDateText).text = dotDate
        findViewById<TextView>(R.id.scheduleDate).text = dotDate
    }

    private fun setupMoodButtons() {
        val moodIds = mapOf(
            R.id.mood1 to 1,
            R.id.mood2 to 2,
            R.id.mood3 to 3,
            R.id.mood4 to 4,
            R.id.mood5 to 5
        )

        moodIds.forEach { (resId, id) ->
            findViewById<TextView>(resId).setOnClickListener {
                currentMoodId = id
                updateMoodLabel(id)
                moodContainer.visibility = View.GONE
                saveDiaryToServer()
            }
        }
    }

    private fun updateMoodLabel(id: Int) {
        selectedMoodText.text = when (id) {
            1 -> "😊 좋은 하루"
            3 -> "😢 슬픈 하루"
            4 -> "😡 화난 하루"
            5 -> "😴 피곤한 하루"
            else -> "😐 보통 하루"
        }
    }

    private fun loadData() {
        fetchDiary()
        fetchSchedulesAndFlatten()
        fetchAllSchedulesForCalendar()
    }

    private fun fetchDiary() {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("auth_token", null) ?: return

        val request = Request.Builder()
            .url("$BASE_URL/diary/$currentSelectedDate")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("DIARY", "일기 불러오기 실패: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    try {
                        val json = JSONObject(body)

                        runOnUiThread {
                            val content = json.optString("content", "")
                            diaryContentText.text =
                                if (content.isEmpty()) "일기를 작성해주세요." else content

                            currentMoodId = json.optInt("mood_id", 2)
                            updateMoodLabel(currentMoodId)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DIARY", "파싱 오류: ${e.message}")
                    }
                } else {
                    android.util.Log.e("DIARY", "응답 실패: ${response.code}")
                }
            }
        })
    }

    private fun fetchSchedulesAndFlatten() {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("auth_token", null) ?: return

        val request = Request.Builder()
            .url("$BASE_URL/schedules/$currentSelectedDate")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("SCHEDULE", "일정 불러오기 실패: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    try {
                        val jsonArray = JSONArray(body)
                        val allTasks = mutableListOf<TaskItem>()

                        for (i in 0 until jsonArray.length()) {
                            val scheduleObj = jsonArray.getJSONObject(i)
                            val tasksArr = scheduleObj.optJSONArray("tasks") ?: JSONArray()

                            for (j in 0 until tasksArr.length()) {
                                val taskObj = tasksArr.getJSONObject(j)

                                allTasks.add(
                                    TaskItem(
                                        taskObj.getInt("id"),
                                        taskObj.getString("content"),
                                        taskObj.optBoolean("is_completed")
                                    )
                                )
                            }
                        }

                        runOnUiThread {
                            studyTaskAdapter.updateData(allTasks)
                        }

                    } catch (e: Exception) {
                        android.util.Log.e("SCHEDULE", "파싱 오류: ${e.message}")
                    }
                } else {
                    android.util.Log.e("SCHEDULE", "응답 실패: ${response.code}")
                }
            }
        })
    }

    private fun showDiaryEditDialog() {
        val editText = EditText(this)
        val currentText = diaryContentText.text.toString()

        editText.setText(
            if (currentText == "일기를 작성해주세요.") "" else currentText
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("한 줄 일기")
            .setView(editText)
            .setPositiveButton("저장") { _, _ ->
                val newContent = editText.text.toString()
                diaryContentText.text =
                    if (newContent.isEmpty()) "일기를 작성해주세요." else newContent

                saveDiaryToServer()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun saveDiaryToServer() {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("auth_token", null) ?: return

        var content = diaryContentText.text.toString()

        if (content == "일기를 작성해주세요.") {
            content = ""
        }

        val json = JSONObject().apply {
            put("target_date", currentSelectedDate)
            put("content", content)
            put("mood_id", currentMoodId)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/diary")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("DIARY", "일기 저장 실패: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@TodayStudyActivity,
                            "저장되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    android.util.Log.e("DIARY", "저장 응답 실패: ${response.code}")
                }
            }
        })
    }

    private fun updateTaskCheckOnServer(taskId: Int, isChecked: Boolean) {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("auth_token", null) ?: return

        val json = JSONObject().apply {
            put("is_completed", if (isChecked) 1 else 0)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/tasks/$taskId/check")
            .patch(requestBody)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("TASK", "체크 상태 변경 실패: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    android.util.Log.e("TASK", "체크 상태 변경 응답 실패: ${response.code}")
                } else {
                    fetchAllSchedulesForCalendar()
                }
            }
        })
    }
}