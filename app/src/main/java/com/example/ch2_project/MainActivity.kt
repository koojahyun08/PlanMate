package com.example.ch2_project

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val token = sharedPreferences.getString("auth_token", null)
        if (token != null) {
            navigateToMenu()
            return
        }

        // 이미지뷰들
        val kakaoImage = findViewById<ImageView>(R.id.kakaoImage)
        val googleImage = findViewById<ImageView>(R.id.googleImage)
        val naverImage = findViewById<ImageView>(R.id.naverImage)

        kakaoImage.setImageResource(R.drawable.kakao_logo)
        googleImage.setImageResource(R.drawable.google_logo)
        naverImage.setImageResource(R.drawable.naver_logo)

        val kakaoButton = findViewById<LinearLayout>(R.id.kakaoLoginButton)
        val googleButton = findViewById<LinearLayout>(R.id.googleLoginButton)
        val naverButton = findViewById<LinearLayout>(R.id.naverLoginButton)
        val loginText = findViewById<TextView>(R.id.loginText)
        val signupText = findViewById<TextView>(R.id.signupText)

        kakaoButton.setOnClickListener {
            openSocialLogin("kakao")
        }

        googleButton.setOnClickListener {
            openSocialLogin("google")
        }

        naverButton.setOnClickListener {
            openSocialLogin("naver")
        }

        loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        signupText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun openSocialLogin(provider: String) {
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra("provider", provider)
        startActivity(intent)
    }

    private fun navigateToMenu() {
        startActivity(Intent(this, AiRecommendationActivity::class.java))
        finish()
    }
}