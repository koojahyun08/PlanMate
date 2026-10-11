package com.example.ch2_project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException

class MemoActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var memoAdapter: MemoAdapter

    private val client = OkHttpClient()
    private val SERVER_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/api/memos"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memo)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        recyclerView = findViewById(R.id.memoRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        memoAdapter = MemoAdapter(
            memoList = mutableListOf(),
            onEditClick = { memo ->
                val intent = Intent(this, AddMemoActivity::class.java)
                intent.putExtra("MEMO_ID", memo.id)
                intent.putExtra("MEMO_TITLE", memo.title)
                intent.putExtra("MEMO_CONTENT", memo.content)
                startActivity(intent)
            },
            onDeleteClick = { memo ->
                AlertDialog.Builder(this)
                    .setTitle("메모 삭제")
                    .setMessage("정말로 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { dialog, _ ->
                        deleteMemo(memo.id)
                        dialog.dismiss()
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        )
        recyclerView.adapter = memoAdapter

        val backButton = findViewById<LinearLayout>(R.id.backButton)
        backButton.setOnClickListener { finish() }

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
                R.id.menu_memo -> drawerLayout.closeDrawer(GravityCompat.END)
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
            startActivity(Intent(this, AddMemoActivity::class.java))
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
            startActivity(Intent(this, TodoListActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_analysis).setOnClickListener {
            startActivity(Intent(this, StudyAnalysisActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_memo).setOnClickListener {
            // 현재 화면
        }
    }

    override fun onResume() {
        super.onResume()
        fetchMemos()
    }

    private fun fetchMemos() {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null) ?: return

        val request = Request.Builder().url(SERVER_URL).get()
            .addHeader("Authorization", "Bearer $token").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MemoActivity, "목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonArray = JSONArray(responseBody)
                        val memos = mutableListOf<Memo>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            memos.add(
                                Memo(
                                    id = obj.getInt("id"),
                                    title = obj.getString("title"),
                                    content = obj.getString("content"),
                                    created_at = obj.optString("created_at", "날짜 없음")
                                )
                            )
                        }
                        android.util.Log.d("MEMO_SUCCESS", "불러온 메모 개수: ${memos.size}")
                        runOnUiThread { memoAdapter.updateData(memos) }
                    } catch (e: Exception) {
                        android.util.Log.e("MEMO_ERROR", "데이터 해독 실패: ${e.message}\n서버가 보낸 원본: $responseBody")
                        runOnUiThread { Toast.makeText(this@MemoActivity, "데이터 변환 오류", Toast.LENGTH_SHORT).show() }
                    }
                } else {
                    android.util.Log.e("MEMO_ERROR", "서버 응답 에러: ${response.code}")
                }
            }
        })
    }

    private fun deleteMemo(memoId: Int) {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null) ?: return

        val request = Request.Builder().url("$SERVER_URL/$memoId").delete()
            .addHeader("Authorization", "Bearer $token").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MemoActivity, "삭제 실패", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MemoActivity, "메모가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        fetchMemos()
                    } else {
                        Toast.makeText(this@MemoActivity, "오류 발생", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}