package com.example.ch2_project

import android.content.Context
import android.os.Bundle
import android.widget.EditText
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

class AddMemoActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    // 🚨 반드시 본인의 현재 ngrok 주소로 변경! (끝에 /api/memos 확인)
    private val SERVER_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev/api/memos"

    private var memoId: Int = -1 // 수정 모드인지 확인하는 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_memo)

        val backButton = findViewById<TextView>(R.id.backButton)
        val saveButton = findViewById<TextView>(R.id.saveButton)
        val titleInput = findViewById<EditText>(R.id.titleInput)
        val contentInput = findViewById<EditText>(R.id.contentInput)

        // 1. MemoActivity에서 보낸 데이터가 있는지 확인 (수정 버튼을 누르고 온 경우)
        memoId = intent.getIntExtra("MEMO_ID", -1)
        if (memoId != -1) {
            titleInput.setText(intent.getStringExtra("MEMO_TITLE") ?: "")
            contentInput.setText(intent.getStringExtra("MEMO_CONTENT") ?: "")
            saveButton.text = "수정완료"
        }

        backButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val content = contentInput.text.toString().trim()

            if (content.isEmpty()) {
                Toast.makeText(this, "내용을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. 모드에 따라 알맞은 서버 요청 보내기
            if (memoId != -1) {
                updateMemoToServer(title, content) // 수정 (PUT)
            } else {
                saveMemoToServer(title, content)   // 새 글 (POST)
            }
        }
    }

    private fun saveMemoToServer(title: String, content: String) {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null)
        if (token == null) return

        val json = JSONObject().apply {
            put("title", title)
            put("content", content)
        }
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url(SERVER_URL).post(requestBody)
            .addHeader("Authorization", "Bearer $token").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@AddMemoActivity, "네트워크 오류", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AddMemoActivity, "저장 완료!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddMemoActivity, "저장 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun updateMemoToServer(title: String, content: String) {
        val token = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("auth_token", null)
        if (token == null) return

        val json = JSONObject().apply {
            put("title", title)
            put("content", content)
        }
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url("$SERVER_URL/$memoId").put(requestBody)
            .addHeader("Authorization", "Bearer $token").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@AddMemoActivity, "네트워크 오류", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AddMemoActivity, "수정 완료!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddMemoActivity, "수정 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}