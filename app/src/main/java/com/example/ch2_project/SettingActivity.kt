package com.example.ch2_project

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import android.widget.Switch

class SettingActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

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
                R.id.menu_account -> startActivity(Intent(this, MyAccountActivity::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        // 뒤로가기
        val backButton = findViewById<LinearLayout>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // 테마
        val themeMenu = findViewById<LinearLayout>(R.id.themeMenu)
        themeMenu.setOnClickListener {
            Toast.makeText(this, "준비중입니다", Toast.LENGTH_SHORT).show()
        }

        // 글꼴
        val fontMenu = findViewById<LinearLayout>(R.id.fontMenu)
        fontMenu.setOnClickListener {
            Toast.makeText(this, "준비중입니다", Toast.LENGTH_SHORT).show()
        }

        // 알림 토글
        val notificationSwitch = findViewById<Switch>(R.id.notificationSwitch)
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "알림이 켜졌습니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "알림이 꺼졌습니다", Toast.LENGTH_SHORT).show()
            }
        }

        // 한 줄 일기 토글
        val diarySwitch = findViewById<Switch>(R.id.diarySwitch)
        diarySwitch.isChecked = true
        diarySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "한 줄 일기가 켜졌습니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "한 줄 일기가 꺼졌습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
}