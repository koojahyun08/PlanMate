package com.example.ch2_project

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ScheduleEditActivity : AppCompatActivity() {

    private val BASE_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/api"
    private val client = OkHttpClient()
    private lateinit var taskContainer: LinearLayout
    private var currentDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_edit)

        currentDate = intent.getStringExtra("date") ?: ""

        val backButton = findViewById<LinearLayout>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        taskContainer = findViewById(R.id.taskContainer)

        val addButton = findViewById<LinearLayout>(R.id.addTaskButton)
        addButton.setOnClickListener { addNewTaskRow() }

        val saveButton = findViewById<LinearLayout>(R.id.saveButton)
        saveButton.setOnClickListener { saveAllTasks() }

        fetchSchedules()
    }

    private fun fetchSchedules() {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null) ?: return
        val request = Request.Builder().url("$BASE_URL/schedules/$currentDate")
            .addHeader("Authorization", "Bearer $token").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val jsonArray = JSONArray(body)
                    runOnUiThread {
                        taskContainer.removeAllViews()
                        for (i in 0 until jsonArray.length()) {
                            val tasksArr = jsonArray.getJSONObject(i).getJSONArray("tasks")
                            for (j in 0 until tasksArr.length()) {
                                val t = tasksArr.getJSONObject(j)
                                addTaskRow(t.getString("content"), t.getInt("id"))
                            }
                        }
                    }
                }
            }
        })
    }

    private fun addTaskRow(content: String = "", taskId: Int = -1) {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }
            background = getDrawable(R.drawable.diary_background)
            setPadding(16, 12, 16, 12)
        }

        // 일정 내용 텍스트
        val contentText = TextView(this).apply {
            text = if (content.isEmpty()) "일정 입력" else content
            textSize = 14f
            setTextColor(if (content.isEmpty()) 0xFFAAAAAA.toInt() else 0xFF000000.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            tag = taskId
        }

        // 수정 버튼
        val editBtn = TextView(this).apply {
            text = "수정"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            setPadding(16, 0, 8, 0)
            setOnClickListener {
                // 수정 다이얼로그
                val editText = EditText(this@ScheduleEditActivity)
                editText.setText(contentText.text.toString())
                android.app.AlertDialog.Builder(this@ScheduleEditActivity)
                    .setTitle("일정 수정")
                    .setView(editText)
                    .setPositiveButton("확인") { _, _ ->
                        val newContent = editText.text.toString().trim()
                        if (newContent.isNotEmpty()) {
                            contentText.text = newContent
                            contentText.setTextColor(0xFF000000.toInt())
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        }

        // 삭제 버튼
        val deleteBtn = TextView(this).apply {
            text = "✕"
            textSize = 16f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(8, 0, 0, 0)
            setOnClickListener { taskContainer.removeView(rowLayout) }
        }

        rowLayout.addView(contentText)
        rowLayout.addView(editBtn)
        rowLayout.addView(deleteBtn)
        taskContainer.addView(rowLayout)
    }

    private fun addNewTaskRow() {
        addTaskRow()
    }

    private fun saveAllTasks() {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null) ?: return
        val tasks = mutableListOf<JSONObject>()

        for (i in 0 until taskContainer.childCount) {
            val row = taskContainer.getChildAt(i) as LinearLayout
            val contentText = row.getChildAt(0) as TextView
            val content = contentText.text.toString().trim()
            if (content.isNotEmpty() && content != "일정 입력") {
                tasks.add(JSONObject().apply {
                    put("content", content)
                    val taskId = contentText.tag as? Int ?: -1
                    if (taskId != -1) put("id", taskId)
                })
            }
        }

        val json = JSONObject().apply {
            put("target_date", currentDate)
            put("title", currentDate)
            put("tasks", JSONArray(tasks.map { it }))
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$BASE_URL/schedules")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $token").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@ScheduleEditActivity, "저장 실패", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@ScheduleEditActivity, "저장되었습니다", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        })
    }
}