package com.example.ch2_project

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class HelpActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

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

        // FAQ 1
        val faq1 = findViewById<LinearLayout>(R.id.faq1)
        faq1.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("기기를 바꾸면 데이터가 사라지나요?")
                .setMessage("아니요! 계정에 로그인하면 데이터가 서버에 저장되어 기기를 바꿔도 유지됩니다.")
                .setPositiveButton("확인", null)
                .show()
        }

        // FAQ 2
        val faq2 = findViewById<LinearLayout>(R.id.faq2)
        faq2.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("AI가 어떻게 계획을 세워주나요?")
                .setMessage("AI 채팅에서 학습 목표나 공부한 내용을 입력하면 AI가 자동으로 맞춤형 학습 플랜을 생성해줍니다.")
                .setPositiveButton("확인", null)
                .show()
        }

        // FAQ 3
        val faq3 = findViewById<LinearLayout>(R.id.faq3)
        faq3.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("알림이 오지 않아요.")
                .setMessage("설정 > 알림에서 알림이 켜져 있는지 확인해주세요. 또한 휴대폰 설정에서 앱 알림이 허용되어 있는지 확인해주세요.")
                .setPositiveButton("확인", null)
                .show()
        }
    }
}