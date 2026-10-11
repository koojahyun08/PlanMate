package com.example.ch2_project

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val googleSignupButton = findViewById<LinearLayout>(R.id.googleSignupButton)
        googleSignupButton.setOnClickListener {
            openSocialSignup("google")
        }

        val kakaoSignupButton = findViewById<LinearLayout>(R.id.kakaoSignupButton)
        kakaoSignupButton.setOnClickListener {
            openSocialSignup("kakao")
        }

        val naverSignupButton = findViewById<LinearLayout>(R.id.naverSignupButton)
        naverSignupButton.setOnClickListener {
            openSocialSignup("naver")
        }

        val emailSignupButton = findViewById<LinearLayout>(R.id.emailSignupButton)
        emailSignupButton.setOnClickListener {
            startActivity(Intent(this, EmailSignupActivity::class.java))
        }
    }

    private fun openSocialSignup(provider: String) {
        // 소셜 회원가입은 자동 로그인 → AiRecommendationActivity로 이동
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra("provider", provider)
        startActivity(intent)
    }
}