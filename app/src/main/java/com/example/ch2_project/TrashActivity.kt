package com.example.ch2_project

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class TrashActivity : AppCompatActivity() {

    private val BASE_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/api"
    private val client = OkHttpClient()

    private lateinit var trashAdapter: TrashAdapter

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var selectAllCheckBox: CheckBox
    private lateinit var deleteSelectedButton: ImageView

    private var isUpdatingSelectAll = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        recyclerView = findViewById(R.id.trashRecyclerView)
        selectAllCheckBox = findViewById(R.id.selectAllCheckBox)
        deleteSelectedButton = findViewById(R.id.deleteSelectedButton)

        recyclerView.layoutManager = LinearLayoutManager(this)

        trashAdapter = TrashAdapter(
            trashList = mutableListOf(),
            onRestoreClick = { id ->
                restoreSchedule(id)
            },
            onCheckedChanged = {
                updateSelectionHeader()
            }
        )

        recyclerView.adapter = trashAdapter

        selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSelectAll) return@setOnCheckedChangeListener

            if (isChecked) {
                trashAdapter.checkAll()
            } else {
                trashAdapter.uncheckAll()
            }
        }

        deleteSelectedButton.setOnClickListener {
            val checkedIds = trashAdapter.getCheckedIds()

            if (checkedIds.isEmpty()) {
                Toast.makeText(this, "삭제할 항목을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("선택 영구 삭제")
                .setMessage("선택한 ${checkedIds.size}개 항목을 영구 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
                .setPositiveButton("삭제") { _, _ ->
                    deleteSelectedSchedules(checkedIds)
                }
                .setNegativeButton("취소", null)
                .show()
        }

        val hamburgerMenu = findViewById<LinearLayout>(R.id.hamburgerMenu)
        hamburgerMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_ai -> startActivity(Intent(this, AiRecommendationActivity::class.java))
                R.id.menu_timer -> startActivity(Intent(this, TimerActivity::class.java))
                R.id.menu_today_study -> startActivity(Intent(this, TodayStudyActivity::class.java))
                R.id.menu_todo_list -> startActivity(Intent(this, TodoListActivity::class.java))
                R.id.menu_analysis -> startActivity(Intent(this, StudyAnalysisActivity::class.java))
                R.id.menu_memo -> startActivity(Intent(this, MemoActivity::class.java))
                R.id.menu_account -> startActivity(Intent(this, MyAccountActivity::class.java))
            }

            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        val backButton = findViewById<LinearLayout>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        updateSelectionHeader()
        loadTrashList()
    }

    private fun updateSelectionHeader() {
        val checkedCount = trashAdapter.getCheckedCount()
        val totalCount = trashAdapter.getTotalCount()

        isUpdatingSelectAll = true
        selectAllCheckBox.isChecked = totalCount > 0 && checkedCount == totalCount
        isUpdatingSelectAll = false

        deleteSelectedButton.visibility = View.VISIBLE

        if (checkedCount > 0) {
            deleteSelectedButton.alpha = 1.0f
            deleteSelectedButton.isEnabled = true
        } else {
            deleteSelectedButton.alpha = 0.35f
            deleteSelectedButton.isEnabled = false
        }
    }

    private fun loadTrashList() {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("auth_token", null) ?: return

        val request = Request.Builder()
            .url("$BASE_URL/schedules/trash/list")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@TrashActivity,
                        "휴지통 목록을 불러오지 못했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    try {
                        val jsonArray = JSONArray(body)
                        val list = mutableListOf<TrashScheduleItem>()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)

                            val tasksArr = obj.optJSONArray("tasks") ?: JSONArray()
                            val taskList = mutableListOf<TaskItem>()

                            for (j in 0 until tasksArr.length()) {
                                val tObj = tasksArr.getJSONObject(j)

                                taskList.add(
                                    TaskItem(
                                        tObj.getInt("id"),
                                        tObj.optString("content", ""),
                                        readCompletedValue(tObj)
                                    )
                                )
                            }

                            list.add(
                                TrashScheduleItem(
                                    scheduleId = obj.getInt("id"),
                                    title = obj.optString("title", "제목 없음"),
                                    deletedAt = obj.optString("deleted_at", ""),
                                    tasks = taskList
                                )
                            )
                        }

                        runOnUiThread {
                            trashAdapter.updateData(list)
                            updateSelectionHeader()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                this@TrashActivity,
                                "휴지통 데이터 파싱 오류가 발생했습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@TrashActivity,
                            "휴지통 목록을 불러오지 못했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun readCompletedValue(obj: JSONObject): Boolean {
        val value = obj.opt("is_completed")

        return when (value) {
            is Boolean -> value
            is Int -> value == 1
            is Number -> value.toInt() == 1
            is String -> value == "1" || value.equals("true", ignoreCase = true)
            else -> false
        }
    }

    private fun restoreSchedule(id: Int) {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("auth_token", null) ?: return

        val requestBody = RequestBody.create(null, ByteArray(0))

        val request = Request.Builder()
            .url("$BASE_URL/schedules/$id/restore")
            .patch(requestBody)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@TrashActivity, "복원에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@TrashActivity, "복원되었습니다.", Toast.LENGTH_SHORT).show()
                        loadTrashList()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@TrashActivity, "복원에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun deleteSelectedSchedules(ids: List<Int>) {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("auth_token", null) ?: return

        if (ids.isEmpty()) return

        val completedCount = AtomicInteger(0)
        val successCount = AtomicInteger(0)

        for (id in ids) {
            val request = Request.Builder()
                .url("$BASE_URL/schedules/$id/permanent")
                .delete()
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    checkDeleteSelectedFinished(
                        completedCount.incrementAndGet(),
                        successCount.get(),
                        ids.size
                    )
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        successCount.incrementAndGet()
                    }

                    checkDeleteSelectedFinished(
                        completedCount.incrementAndGet(),
                        successCount.get(),
                        ids.size
                    )
                }
            })
        }
    }

    private fun checkDeleteSelectedFinished(
        completedCount: Int,
        successCount: Int,
        totalCount: Int
    ) {
        if (completedCount == totalCount) {
            runOnUiThread {
                if (successCount > 0) {
                    Toast.makeText(
                        this,
                        "${successCount}개 영구 삭제되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "영구 삭제에 실패했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                loadTrashList()
            }
        }
    }
}