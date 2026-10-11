package com.example.ch2_project

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MyAccountActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var nameText: TextView
    private lateinit var emailText: TextView

    private val client = OkHttpClient()

    private val BASE_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/api"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_account)

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        nameText = findViewById(R.id.nameText)
        emailText = findViewById(R.id.emailText)

        fetchMyProfile()

        // 햄버거 메뉴
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
                R.id.menu_account -> drawerLayout.closeDrawer(GravityCompat.END)
            }

            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        // 설정
        val settingMenu = findViewById<LinearLayout>(R.id.settingMenu)
        settingMenu.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        // 도움말/FAQ
        val faqMenu = findViewById<LinearLayout>(R.id.faqMenu)
        faqMenu.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        // 회원탈퇴
        val withdrawMenu = findViewById<LinearLayout>(R.id.withdrawMenu)
        withdrawMenu.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("회원탈퇴")
                .setMessage("정말 탈퇴하시겠습니까?\n탈퇴 후 모든 데이터가 삭제됩니다.")
                .setPositiveButton("탈퇴") { _, _ ->
                    Toast.makeText(this, "준비중입니다", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        // 로그아웃
        val logoutMenu = findViewById<LinearLayout>(R.id.logoutMenu)
        logoutMenu.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃") { _, _ ->
                    sharedPreferences.edit().clear().apply()

                    android.webkit.CookieManager.getInstance().removeAllCookies(null)
                    android.webkit.CookieManager.getInstance().flush()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchMyProfile()
    }

    private fun fetchMyProfile() {
        val token = sharedPreferences.getString("auth_token", null)

        if (token.isNullOrBlank()) {
            nameText.text = "게스트"
            emailText.text = "-"
            return
        }

        val request = Request.Builder()
            .url("$BASE_URL/me")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    nameText.text = "게스트"
                    emailText.text = "-"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        try {
                            val jsonObject = JSONObject(body)

                            val name = jsonObject.optString("name", "")
                            val email = jsonObject.optString("email", "")

                            nameText.text = if (name.isBlank()) "게스트" else name
                            emailText.text = if (email.isBlank()) "-" else email

                        } catch (e: Exception) {
                            nameText.text = "게스트"
                            emailText.text = "-"
                        }
                    } else {
                        nameText.text = "게스트"
                        emailText.text = "-"
                    }
                }
            }
        })
    }
}