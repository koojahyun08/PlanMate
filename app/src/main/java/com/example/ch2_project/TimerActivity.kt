package com.example.ch2_project

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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
import kotlin.math.abs

data class TimerTask(val id: Int, val content: String, val parsedSubject: String, val parsedSeconds: Int)

class TimerActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var timerText: TextView
    private lateinit var subjectInput: EditText

    private lateinit var startButton: LinearLayout
    private lateinit var pauseButton: LinearLayout
    private lateinit var stopButton: LinearLayout
    private lateinit var resetButton: LinearLayout

    private lateinit var goalTitleText: TextView
    private lateinit var timerProgressBar: ProgressBar
    private lateinit var completedTimeText: TextView
    private lateinit var remainingTimeText: TextView
    private lateinit var tasksRecyclerView: RecyclerView

    private val client = OkHttpClient()
    private val BASE_API_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/api"
    private val STUDY_RECORD_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/study-records"

    private var isRunning = false
    private var targetSeconds = 0
    private var remainingSeconds = 0
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var hasStartedSession = false

    private var selectedTaskId: Int? = null
    private var selectedTaskSubject: String = ""

    private val tasksList = mutableListOf<TimerTask>()
    private lateinit var taskAdapter: TimerTaskAdapter

    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        initViews()
        setupSidebar()
        setupButtons()
        setupRecyclerView()

        val backButton = findViewById<LinearLayout>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        loadTodayTasks()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            if (!hasStartedSession || remainingSeconds <= 0) return

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val isFlat = abs(x) < 2.5f && abs(y) < 2.5f && abs(z) > 8.0f

            if (isFlat && !isRunning) {
                isRunning = true
                timerText.setTextColor(Color.parseColor("#000000"))
                runTimer()
            } else if (!isFlat && isRunning) {
                isRunning = false
                handler.removeCallbacks(timerRunnable)
                timerText.setTextColor(Color.parseColor("#FF0000"))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        timerText = findViewById(R.id.timerText)
        subjectInput = findViewById(R.id.subjectInput)

        startButton = findViewById(R.id.startButton)
        pauseButton = findViewById(R.id.pauseButton)
        stopButton = findViewById(R.id.stopButton)
        resetButton = findViewById(R.id.resetButton)

        goalTitleText = findViewById(R.id.goalTitleText)
        timerProgressBar = findViewById(R.id.timerProgressBar)
        completedTimeText = findViewById(R.id.completedTimeText)
        remainingTimeText = findViewById(R.id.remainingTimeText)
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
    }

    private fun showTimePickerDialog() {
        if (isRunning) {
            Toast.makeText(this, "타이머를 먼저 정지해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        val hourPicker = NumberPicker(this).apply {
            minValue = 0
            maxValue = 23
            value = if (targetSeconds > 0) targetSeconds / 3600 else 0
        }

        val hourLabel = TextView(this).apply {
            text = " 시간  "
            textSize = 18f
            setTextColor(Color.BLACK)
        }

        val minutePicker = NumberPicker(this).apply {
            minValue = 0
            maxValue = 59
            value = if (targetSeconds > 0) (targetSeconds % 3600) / 60 else 0
        }

        val minuteLabel = TextView(this).apply {
            text = " 분"
            textSize = 18f
            setTextColor(Color.BLACK)
        }

        linearLayout.addView(hourPicker)
        linearLayout.addView(hourLabel)
        linearLayout.addView(minutePicker)
        linearLayout.addView(minuteLabel)

        AlertDialog.Builder(this)
            .setTitle("목표 시간 설정")
            .setView(linearLayout)
            .setPositiveButton("확인") { _, _ ->
                val h = hourPicker.value
                val m = minutePicker.value

                targetSeconds = h * 3600 + m * 60
                remainingSeconds = targetSeconds
                selectedTaskId = null

                val currentSubject = subjectInput.text.toString().trim()
                val displaySubject = if (currentSubject.isNotEmpty()) currentSubject else "미지정"

                goalTitleText.text = "오늘의 목표: $displaySubject ${formatTime(targetSeconds)}"
                timerText.text = formatTime(remainingSeconds)

                timerProgressBar.max = targetSeconds
                timerProgressBar.progress = remainingSeconds
                updateProgressUI()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun resetToInitialState() {
        subjectInput.setText("")
        timerText.text = "00:00:00"
        timerText.setTextColor(Color.parseColor("#000000"))
        goalTitleText.text = "오늘의 목표를 선택하거나 설정해주세요"
        completedTimeText.text = "00:00:00 완료"
        remainingTimeText.text = "00:00:00 남음"
        timerProgressBar.progress = 0
        timerProgressBar.max = 100

        selectedTaskId = null
        selectedTaskSubject = ""
        targetSeconds = 0
        remainingSeconds = 0
        hasStartedSession = false
    }

    private fun showLoadingDialog() {
        if (loadingDialog != null) return
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(50, 50, 50, 50)
            gravity = android.view.Gravity.CENTER
        }
        val progressBar = ProgressBar(this).apply {
            isIndeterminate = true
            setPadding(0, 0, 30, 0)
        }
        val tvText = TextView(this).apply {
            text = "서버에 저장 중입니다..."
            setTextColor(Color.parseColor("#000000"))
            textSize = 16f
        }
        ll.addView(progressBar)
        ll.addView(tvText)

        loadingDialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setView(ll)
            .create()
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun setupRecyclerView() {
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TimerTaskAdapter(tasksList) { task ->
            if (isRunning) {
                Toast.makeText(this, "먼저 타이머를 정지해주세요.", Toast.LENGTH_SHORT).show()
                return@TimerTaskAdapter
            }

            selectedTaskId = task.id
            selectedTaskSubject = task.parsedSubject
            targetSeconds = task.parsedSeconds
            remainingSeconds = task.parsedSeconds

            subjectInput.setText(task.parsedSubject)

            goalTitleText.text = "오늘의 목표: ${task.parsedSubject} ${formatTime(targetSeconds)}"
            timerText.text = formatTime(remainingSeconds)

            timerProgressBar.max = targetSeconds
            timerProgressBar.progress = targetSeconds
            updateProgressUI()
        }
        tasksRecyclerView.adapter = taskAdapter
    }

    private fun setupButtons() {
        timerText.setOnClickListener { showTimePickerDialog() }

        startButton.setOnClickListener {
            if (!isRunning) {
                val currentSubject = subjectInput.text.toString().trim()
                if (currentSubject.isEmpty()) {
                    Toast.makeText(this, "과목명을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (targetSeconds <= 0) {
                    Toast.makeText(this, "타이머 글씨를 터치하여 시간을 설정해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (currentSubject != selectedTaskSubject) {
                    selectedTaskSubject = currentSubject
                    goalTitleText.text = "오늘의 목표: $currentSubject ${formatTime(targetSeconds)}"
                }
                if (remainingSeconds <= 0) {
                    Toast.makeText(this, "이미 완료된 목표입니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                isRunning = true
                hasStartedSession = true
                timerText.setTextColor(Color.parseColor("#000000"))
                runTimer()
                Toast.makeText(this, "휴대폰을 책상에 평평하게 놓으면 시간이 흐릅니다!", Toast.LENGTH_LONG).show()
            }
        }

        pauseButton.setOnClickListener {
            isRunning = false
            hasStartedSession = false
            handler.removeCallbacks(timerRunnable)
            timerText.setTextColor(Color.parseColor("#000000"))
            Toast.makeText(this, "일시정지 (다시 시작하려면 버튼을 누르세요)", Toast.LENGTH_SHORT).show()
        }

        stopButton.setOnClickListener {
            if (!isRunning && remainingSeconds == targetSeconds) return@setOnClickListener
            isRunning = false
            hasStartedSession = false
            handler.removeCallbacks(timerRunnable)

            val elapsedSeconds = targetSeconds - remainingSeconds
            if (elapsedSeconds <= 0) {
                Toast.makeText(this, "기록할 진행 시간이 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoadingDialog()
            saveRecordToServer(selectedTaskSubject, elapsedSeconds) {
                if (selectedTaskId != null) {
                    updateTaskRemainingTime(selectedTaskId!!, remainingSeconds)
                } else {
                    runOnUiThread {
                        hideLoadingDialog()
                        Toast.makeText(this@TimerActivity, "수동 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        resetToInitialState()
                    }
                }
            }
        }

        resetButton.setOnClickListener {
            isRunning = false
            hasStartedSession = false
            handler.removeCallbacks(timerRunnable)
            resetToInitialState()
            Toast.makeText(this, "초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRunning && remainingSeconds > 0) {
                remainingSeconds--
                timerText.text = formatTime(remainingSeconds)
                updateProgressUI()
                handler.postDelayed(this, 1000)
            } else if (isRunning && remainingSeconds <= 0) {
                isRunning = false
                hasStartedSession = false
                timerText.text = "00:00:00"
                timerText.setTextColor(Color.parseColor("#000000"))
                updateProgressUI()
                Toast.makeText(this@TimerActivity, "목표 시간을 모두 달성했습니다!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun runTimer() {
        handler.removeCallbacks(timerRunnable)
        handler.post(timerRunnable)
    }

    private fun updateProgressUI() {
        timerProgressBar.progress = remainingSeconds
        val elapsed = targetSeconds - remainingSeconds
        completedTimeText.text = "${formatTime(elapsed)} 완료"
        remainingTimeText.text = "${formatTime(remainingSeconds)} 남음"
    }

    private fun formatTime(totalSecs: Int): String {
        if (totalSecs <= 0) return "00:00:00"
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun loadTodayTasks() {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null) ?: return
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val request = Request.Builder()
            .url("$BASE_API_URL/schedules/$todayStr")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return
                    val schedules = JSONArray(body)
                    tasksList.clear()

                    for (i in 0 until schedules.length()) {
                        val tasksArr = schedules.getJSONObject(i).optJSONArray("tasks") ?: continue
                        for (j in 0 until tasksArr.length()) {
                            val tObj = tasksArr.getJSONObject(j)
                            val id = tObj.getInt("id")
                            val content = tObj.getString("content")
                            val isCompleted = tObj.optBoolean("is_completed", false)

                            var subj = tObj.optString("subject", "")
                            var secs = if (tObj.has("target_seconds") && !tObj.isNull("target_seconds")) {
                                tObj.getInt("target_seconds")
                            } else { 0 }

                            if (subj.isEmpty() || !tObj.has("target_seconds")) {
                                val parsed = parseTaskContentLocal(content)
                                if (subj.isEmpty()) subj = parsed.first
                                if (secs == 0) secs = parsed.second
                            }

                            if (!isCompleted) {
                                tasksList.add(TimerTask(id, content, subj, secs))
                            }
                        }
                    }
                    runOnUiThread { taskAdapter.notifyDataSetChanged() }
                }
            }
        })
    }

    private fun parseTaskContentLocal(content: String): Pair<String, Int> {
        val regex = Regex("(?:(\\d+)시간)?\\s*(?:(\\d+)분)?\\s*(?:(\\d+)초)?$")
        val match = regex.find(content)
        var subject = content.trim()
        var totalSecs = 3600

        if (match != null && match.value.isNotBlank()) {
            val h = match.groups[1]?.value?.toInt() ?: 0
            val m = match.groups[2]?.value?.toInt() ?: 0
            val s = match.groups[3]?.value?.toInt() ?: 0
            totalSecs = (h * 3600) + (m * 60) + s
            subject = content.substring(0, match.range.first).trim()
            if (subject.isEmpty()) subject = content.trim()
        }
        return Pair(subject, totalSecs)
    }

    private fun updateTaskRemainingTime(taskId: Int, remainingSecs: Int) {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null) ?: return

        val json = JSONObject().apply {
            put("target_seconds", remainingSecs)
            if (remainingSecs <= 0) put("is_completed", true)
        }
        val reqBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_API_URL/tasks/$taskId")
            .patch(reqBody)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideLoadingDialog()
                    Toast.makeText(this@TimerActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    hideLoadingDialog()
                    Toast.makeText(this@TimerActivity, "할 일의 남은 시간이 업데이트 되었습니다.", Toast.LENGTH_SHORT).show()
                    loadTodayTasks()
                    resetToInitialState()
                }
            }
        })
    }

    private fun saveRecordToServer(subject: String, durationSeconds: Int, onSuccess: () -> Unit) {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null) ?: return
        val json = JSONObject().apply {
            put("subject", subject)
            put("duration_seconds", durationSeconds)
        }
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(STUDY_RECORD_URL)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideLoadingDialog()
                    Toast.makeText(this@TimerActivity, "기록 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                onSuccess()
            }
        })
    }

    private fun setupSidebar() {
        val hamburgerMenu = findViewById<LinearLayout>(R.id.hamburgerMenu)
        hamburgerMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.END) }
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_ai -> startActivity(Intent(this, AiRecommendationActivity::class.java))
                R.id.menu_timer -> drawerLayout.closeDrawer(GravityCompat.END)
                R.id.menu_today_study -> startActivity(Intent(this, TodayStudyActivity::class.java))
                R.id.menu_todo_list -> startActivity(Intent(this, TodoListActivity::class.java))
                R.id.menu_analysis -> startActivity(Intent(this, StudyAnalysisActivity::class.java))
                R.id.menu_memo -> startActivity(Intent(this, MemoActivity::class.java))
                R.id.menu_account -> startActivity(Intent(this, MyAccountActivity::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        // 하단바
        findViewById<LinearLayout>(R.id.nav_ai).setOnClickListener {
            startActivity(Intent(this, AiRecommendationActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_schedule).setOnClickListener {
            startActivity(Intent(this, TodayStudyActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_timer).setOnClickListener {
            // 현재 화면
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
    }

    inner class TimerTaskAdapter(
        private val list: List<TimerTask>,
        private val onClick: (TimerTask) -> Unit
    ) : RecyclerView.Adapter<TimerTaskAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val contentText: TextView = view.findViewById(R.id.taskContentText)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timer_task, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.contentText.text = item.content
            holder.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = list.size
    }
}