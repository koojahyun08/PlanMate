package com.example.ch2_project

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

class AddTodoActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val BASE_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/api/schedules"
    private var selectedDate: String = ""
    private var isEditMode = false
    private var editScheduleId = -1
    private lateinit var tasksContainer: LinearLayout
    private lateinit var titleInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_todo)

        val activityTitle = findViewById<TextView>(R.id.activityTitle) // 상단 타이틀
        val dateText = findViewById<TextView>(R.id.dateText)
        val dateSettingButton = findViewById<TextView>(R.id.timeSettingButton)
        titleInput = findViewById<EditText>(R.id.titleInput)
        tasksContainer = findViewById<LinearLayout>(R.id.tasksContainer)
        val addTaskLineButton = findViewById<TextView>(R.id.addTaskLineButton)
        val saveButton = findViewById<TextView>(R.id.addButton)

        findViewById<TextView>(R.id.backButton).setOnClickListener { finish() }

        dateSettingButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val fm = String.format("%02d", month + 1)
                val fd = String.format("%02d", dayOfMonth)
                selectedDate = "$year-$fm-$fd"
                dateText.text = "$year.$fm.$fd"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        tasksContainer.removeAllViews()
        isEditMode = intent.getBooleanExtra("isEditMode", false)

        if (isEditMode) {
            // 🚀 수정 모드일 때 텍스트 변경 로직 복구
            activityTitle.text = "할 일 수정하기"
            saveButton.text = "수정하기"

            editScheduleId = intent.getIntExtra("scheduleId", -1)
            titleInput.setText(intent.getStringExtra("title"))
            selectedDate = intent.getStringExtra("targetDate") ?: ""
            dateText.text = selectedDate.replace("-", ".")
            val tasksStr = intent.getStringExtra("tasks")
            if (tasksStr != null) {
                val tasksArray = JSONArray(tasksStr)
                for (i in 0 until tasksArray.length()) {
                    val obj = tasksArray.getJSONObject(i)
                    addNewTaskRow(obj.getString("content"), obj.getInt("id"))
                }
            }
        } else {
            // 추가 모드일 때
            activityTitle.text = "할 일 추가하기"
            val today = Calendar.getInstance()
            selectedDate = String.format("%d-%02d-%02d", today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH))
            dateText.text = selectedDate.replace("-", ".")
            addNewTaskRow()
        }

        addTaskLineButton.setOnClickListener { addNewTaskRow() }
        saveButton.setOnClickListener { saveSchedule() }
    }

    private fun addNewTaskRow(content: String = "", taskId: Int? = null) {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }
        val editText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            hint = "할 일 내용"
            textSize = 16f
            setBackgroundResource(0)
            setText(content)
            tag = taskId
        }
        rowLayout.addView(editText)
        if (tasksContainer.childCount > 0) {
            val deleteBtn = ImageButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(60, 60)
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                background = null
                setOnClickListener { tasksContainer.removeView(rowLayout) }
            }
            rowLayout.addView(deleteBtn)
        }
        tasksContainer.addView(rowLayout)
    }

    private fun saveSchedule() {
        val title = titleInput.text.toString().trim()
        if (title.isEmpty()) return
        val tasksArray = JSONArray()
        for (i in 0 until tasksContainer.childCount) {
            val row = tasksContainer.getChildAt(i) as LinearLayout
            val edit = row.getChildAt(0) as EditText
            val taskContent = edit.text.toString().trim()
            if (taskContent.isNotEmpty()) {
                tasksArray.put(JSONObject().apply {
                    put("content", taskContent)
                    put("is_completed", false)
                    if (edit.tag != null) put("id", edit.tag as Int)
                })
            }
        }
        val json = JSONObject().apply {
            put("target_date", selectedDate); put("title", title); put("tasks", tasksArray)
        }
        sendDataToServer(if (isEditMode) "$BASE_URL/$editScheduleId" else BASE_URL, json.toString(), if (isEditMode) "PUT" else "POST")
    }

    private fun sendDataToServer(url: String, jsonString: String, method: String) {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null) ?: return
        val body = jsonString.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $token").method(method, body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) runOnUiThread { finish() }
            }
        })
    }
}