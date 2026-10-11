package com.example.ch2_project

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences
    private val BASE_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        // 딥링크로 들어온 경우 토큰 처리
        val deepLinkUri = intent?.data
        if (deepLinkUri != null && deepLinkUri.scheme == "studydiary") {
            val token = deepLinkUri.getQueryParameter("token")
            if (token != null) {
                saveToken(token)
                navigateToMenu()
                return
            } else {
                Toast.makeText(this, "로그인 실패: 토큰을 받지 못했습니다", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 10; Android SDK built for x86) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Mobile Safari/537.36"

        val provider = intent.getStringExtra("provider") ?: "google"

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                android.util.Log.e("WEBVIEW_URL", url)

                if (url.startsWith("studydiary://")) {
                    val uri = Uri.parse(url)
                    val token = uri.getQueryParameter("token")
                    if (token != null) {
                        saveToken(token)
                        navigateToMenu()
                    } else {
                        Toast.makeText(this@WebViewActivity, "로그인 실패: 토큰을 받지 못했습니다", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return true
                }

                return false
            }
        }

        val loginUrl = "$BASE_URL/auth/login/$provider"
        webView.loadUrl(loginUrl)
    }

    private fun saveToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).apply()
        Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMenu() {
        val intent = Intent(this, AiRecommendationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}