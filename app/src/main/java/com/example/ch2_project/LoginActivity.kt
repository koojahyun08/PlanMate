package com.example.ch2_project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val SERVER_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/auth/login"

    // 테스트 계정
    private val TEST_ID = "admin"
    private val TEST_PW = "admin1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<LinearLayout>(R.id.loginButton)
        val signupButton = findViewById<TextView>(R.id.signupButton)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 테스트 계정 로그인
            if (email == TEST_ID && password == TEST_PW) {
                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("auth_token", "test_token_admin")
                    .apply()
                Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                navigateToMenu()
                return@setOnClickListener
            }

            val json = JSONObject()
            json.put("email", email)
            json.put("password", password)
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread { Toast.makeText(this@LoginActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show() }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    runOnUiThread {
                        if (response.isSuccessful && responseBody != null) {
                            val jsonResponse = JSONObject(responseBody)
                            val token = jsonResponse.getString("access_token")
                            getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("auth_token", token)
                                .apply()
                            Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            navigateToMenu()
                        } else {
                            Toast.makeText(this@LoginActivity, "로그인 실패: 이메일이나 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }

        signupButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun navigateToMenu() {
        startActivity(Intent(this, AiRecommendationActivity::class.java))
        finish()
    }
}