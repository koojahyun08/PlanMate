package com.example.ch2_project

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // 오늘의 학습
        val todayStudyMenu = findViewById<TextView>(R.id.todayStudyMenu)
        todayStudyMenu.setOnClickListener {
            val intent = Intent(this, TodayStudyActivity::class.java)
            startActivity(intent)
        }

        // 학습 분석
        val studyAnalysisMenu = findViewById<TextView>(R.id.studyAnalysisMenu)
        studyAnalysisMenu.setOnClickListener {
            val intent = Intent(this, StudyAnalysisActivity::class.java)
            startActivity(intent)
        }

        // 타이머
        val timerMenu = findViewById<TextView>(R.id.timerMenu)
        timerMenu.setOnClickListener {
            val intent = Intent(this, TimerActivity::class.java)
            startActivity(intent)
        }

        // AI 추천
        val aiRecommendationMenu = findViewById<TextView>(R.id.aiRecommendationMenu)
        aiRecommendationMenu.setOnClickListener {
            val intent = Intent(this, AiRecommendationActivity::class.java)
            startActivity(intent)
        }
    }
}