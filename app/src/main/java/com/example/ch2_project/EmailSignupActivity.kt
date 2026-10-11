package com.example.ch2_project

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
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

class EmailSignupActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val SERVER_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/auth/signup"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_signup)

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val passwordConfirmInput = findViewById<EditText>(R.id.passwordConfirmInput)
        val signupButton = findViewById<LinearLayout>(R.id.signupButton)

        signupButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val passwordConfirm = passwordConfirmInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != passwordConfirm) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val json = JSONObject()
            json.put("email", email)
            json.put("name", name)
            json.put("password", password)
            json.put("password_confirm", passwordConfirm)

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread { Toast.makeText(this@EmailSignupActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show() }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this@EmailSignupActivity, "회원가입 완료! 로그인해주세요.", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@EmailSignupActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@EmailSignupActivity, "가입 실패 (${response.code})", Toast.LENGTH_SHORT).show()
                            android.util.Log.e("SIGNUP_ERROR", "거절 사유: $responseBody")
                        }
                    }
                }
            })
        }
    }
}