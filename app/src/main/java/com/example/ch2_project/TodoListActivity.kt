package com.example.ch2_project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class TodoListActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var todoAdapter: TodoAdapter

    private val client = OkHttpClient()
    private val BASE_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/api"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_list)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        recyclerView = findViewById(R.id.todoRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        todoAdapter = TodoAdapter(
            scheduleList = mutableListOf(),
            onCheckChanged = { taskId, isChecked ->
                updateTaskCheckOnServer(taskId, isChecked)
            },
            onEditSchedule = { schedule ->
                val intent = Intent(this, AddTodoActivity::class.java).apply {
                    putExtra("isEditMode", true)
                    putExtra("scheduleId", schedule.scheduleId)
                    putExtra("title", schedule.title)
                    putExtra("targetDate", schedule.targetDate)
                    val tasksJson = JSONArray()
                    schedule.tasks.forEach {
                        tasksJson.put(JSONObject().apply {
                            put("id", it.taskId)
                            put("content", it.content)
                            put("is_completed", it.isCompleted)
                        })
                    }
                    putExtra("tasks", tasksJson.toString())
                }
                startActivity(intent)
            },
            onDeleteSchedule = { scheduleId ->
                deleteScheduleOnServer(scheduleId)
            }
        )

        recyclerView.adapter = todoAdapter

        val backButton = findViewById<LinearLayout>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val hamburgerMenu = findViewById<LinearLayout>(R.id.hamburgerMenu)
        hamburgerMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.END) }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_ai -> startActivity(Intent(this, AiRecommendationActivity::class.java))
                R.id.menu_timer -> startActivity(Intent(this, TimerActivity::class.java))
                R.id.menu_today_study -> startActivity(Intent(this, TodayStudyActivity::class.java))
                R.id.menu_todo_list -> drawerLayout.closeDrawer(GravityCompat.END)
                R.id.menu_analysis -> startActivity(Intent(this, StudyAnalysisActivity::class.java))
                R.id.menu_memo -> startActivity(Intent(this, MemoActivity::class.java))
                R.id.menu_account -> startActivity(Intent(this, MyAccountActivity::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        val trashButton = findViewById<ImageView>(R.id.trashButton)
        trashButton.setOnClickListener {
            startActivity(Intent(this, TrashActivity::class.java))
        }

        val addButton = findViewById<FloatingActionButton>(R.id.addButton)
        addButton.setOnClickListener {
            startActivity(Intent(this, AddTodoActivity::class.java))
        }

        // 하단바
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
            // 현재 화면
        }
        findViewById<LinearLayout>(R.id.nav_analysis).setOnClickListener {
            startActivity(Intent(this, StudyAnalysisActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_memo).setOnClickListener {
            startActivity(Intent(this, MemoActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        fetchAllTodos()
    }

    private fun fetchAllTodos() {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null) ?: return

        val request = Request.Builder().url("$BASE_URL/schedules").get()
            .addHeader("Authorization", "Bearer $token").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@TodoListActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonArray = JSONArray(responseBody)
                            val schedules = mutableListOf<ScheduleItem>()
                            for (i in 0 until jsonArray.length()) {
                                val scheduleObj = jsonArray.getJSONObject(i)
                                val scheduleId = scheduleObj.getInt("id")
                                val scheduleTitle = scheduleObj.getString("title")
                                val targetDate = scheduleObj.getString("target_date")
                                val tasksArray = scheduleObj.getJSONArray("tasks")
                                val taskList = mutableListOf<TaskItem>()
                                for (j in 0 until tasksArray.length()) {
                                    val taskObj = tasksArray.getJSONObject(j)
                                    taskList.add(TaskItem(
                                        taskId = taskObj.getInt("id"),
                                        content = taskObj.getString("content"),
                                        isCompleted = taskObj.optBoolean("is_completed", false)
                                    ))
                                }
                                schedules.add(ScheduleItem(scheduleId, targetDate, scheduleTitle, taskList))
                            }
                            todoAdapter.updateData(schedules)
                        } catch (e: Exception) {
                            android.util.Log.e("TODO_ERROR", "파싱 에러: ${e.message}\n원본: $responseBody")
                            Toast.makeText(this@TodoListActivity, "데이터 변환 오류", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@TodoListActivity, "목록 불러오기 실패 (${response.code})", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun updateTaskCheckOnServer(taskId: Int, isCompleted: Boolean) {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null) ?: return
        val json = JSONObject().apply { put("is_completed", isCompleted) }
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$BASE_URL/tasks/$taskId/check").patch(requestBody)
            .addHeader("Authorization", "Bearer $token").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {}
        })
    }

    private fun deleteScheduleOnServer(scheduleId: Int) {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null) ?: return
        val request = Request.Builder().url("$BASE_URL/schedules/$scheduleId").delete()
            .addHeader("Authorization", "Bearer $token").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@TodoListActivity, "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        fetchAllTodos()
                    }
                }
            }
        })
    }

    private fun updateTaskContentOnServer(taskId: Int, newContent: String) {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null) ?: return
        val json = JSONObject().apply { put("content", newContent) }
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$BASE_URL/tasks/$taskId").patch(requestBody)
            .addHeader("Authorization", "Bearer $token").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@TodoListActivity, "수정되었습니다.", Toast.LENGTH_SHORT).show()
                        fetchAllTodos()
                    }
                }
            }
        })
    }
}