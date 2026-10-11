package com.example.ch2_project

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.graphics.drawable.GradientDrawable
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

data class ChatMessage(val isUser: Boolean, val message: String)

object ChatHistoryManager {
    val messages = mutableListOf<ChatMessage>()
}

class AiRecommendationActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: NestedScrollView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var micButton: ImageView
    private lateinit var clearChatButton: ImageView

    private var loadingView: LinearLayout? = null
    private val handler = Handler(Looper.getMainLooper())
    private var dotAnimRunnable: Runnable? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val RECORD_AUDIO_PERMISSION_CODE = 1001

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val BASE_URL = "https://phonogramic-eustolia-anthropologically.ngrok-free.dev"
    private val SERVER_URL = "$BASE_URL/chat"
    private val CLEAR_CHAT_URL = "$BASE_URL/chat/history"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_ai_recommendation)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        chatContainer = findViewById(R.id.chatContainer)
        chatScrollView = findViewById(R.id.chatScrollView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        micButton = findViewById(R.id.micButton)
        clearChatButton = findViewById(R.id.clearChatButton)

        restoreChatHistory()

        val hamburgerMenu = findViewById<LinearLayout>(R.id.hamburgerMenu)
        hamburgerMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        clearChatButton.setOnClickListener {
            showClearChatDialog()
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_ai -> drawerLayout.closeDrawer(GravityCompat.END)
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

        sendButton.setOnClickListener {
            val userMessage = messageInput.text.toString().trim()

            if (userMessage.isNotEmpty()) {
                addUserMessage(userMessage)
                messageInput.text.clear()
                showLoadingBubble()
                scrollToBottom()
                sendMessageToServer(userMessage)
            }
        }

        // 마이크 버튼
        micButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_PERMISSION_CODE
                )
            } else {
                if (isListening) {
                    stopListening()
                } else {
                    startListening()
                }
            }
        }
    }

    private fun showClearChatDialog() {
        AlertDialog.Builder(this)
            .setTitle("채팅 초기화")
            .setMessage("현재 채팅 내역을 삭제하고 처음 화면으로 되돌릴까요?")
            .setPositiveButton("초기화") { _, _ ->
                clearChatHistory()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun clearChatHistory() {
        val token = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .getString("auth_token", null)

        // 화면에 보이는 채팅 내역 먼저 초기화
        resetChatScreen()

        // 서버 토큰이 없으면 화면 초기화만 수행
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "채팅 화면이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = Request.Builder()
            .url(CLEAR_CHAT_URL)
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@AiRecommendationActivity,
                        "화면은 초기화되었지만 서버 기록 삭제에 실패했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AiRecommendationActivity,
                            "채팅 내역이 초기화되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@AiRecommendationActivity,
                            "화면은 초기화되었지만 서버 기록 삭제 중 오류가 발생했습니다. (${response.code})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun resetChatScreen() {
        // 로컬에 저장된 앱 실행 중 채팅 기록 삭제
        ChatHistoryManager.messages.clear()

        // 현재 화면의 말풍선 전부 제거
        chatContainer.removeAllViews()

        // 로딩 말풍선이 남아 있으면 제거
        hideLoadingBubble()

        // 초기 안내 문구 다시 표시
        addAiMessage("어떤 것을 도와드릴까요?", saveToHistory = false)

        scrollToBottom()
    }

    private fun restoreChatHistory() {
        if (ChatHistoryManager.messages.isEmpty()) {
            addAiMessage("어떤 것을 도와드릴까요?", saveToHistory = false)
        } else {
            for (chat in ChatHistoryManager.messages) {
                if (chat.isUser) {
                    addUserMessage(chat.message, saveToHistory = false)
                } else {
                    addAiMessage(chat.message, saveToHistory = false)
                }
            }
        }

        chatScrollView.postDelayed({ scrollToBottom() }, 100)
    }

    private fun startListening() {
        isListening = true
        micButton.setColorFilter(android.graphics.Color.RED)
        Toast.makeText(this, "듣고 있어요...", Toast.LENGTH_SHORT).show()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (!matches.isNullOrEmpty()) {
                    messageInput.setText(matches[0])
                    messageInput.setSelection(messageInput.text.length)
                }

                stopListening()
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (!matches.isNullOrEmpty()) {
                    messageInput.setText(matches[0])
                    messageInput.setSelection(messageInput.text.length)
                }
            }

            override fun onError(error: Int) {
                Toast.makeText(
                    this@AiRecommendationActivity,
                    "음성인식 오류가 발생했습니다.",
                    Toast.LENGTH_SHORT
                ).show()

                stopListening()
            }

            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        isListening = false
        micButton.clearColorFilter()
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                Toast.makeText(this, "마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        dotAnimRunnable?.let {
            handler.removeCallbacks(it)
        }

        speechRecognizer?.destroy()
    }

    private fun scrollToBottom() {
        chatScrollView.post {
            chatScrollView.fullScroll(NestedScrollView.FOCUS_DOWN)
        }
    }

    private fun showLoadingBubble() {
        val outerLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 40)
            }

            gravity = android.view.Gravity.START
        }

        val bubbleLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(50.dpToPx(), 30.dpToPx())
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(12.dpToPx(), 0, 12.dpToPx(), 0)

            val bg = GradientDrawable().apply {
                setColor(0xFFCEE8F1.toInt())
                cornerRadius = 30f
            }

            background = bg
        }

        val dot1 = createDot(6.dpToPx())
        val dot2 = createDot(6.dpToPx())
        val dot3 = createDot(6.dpToPx())

        bubbleLayout.addView(dot1)
        bubbleLayout.addView(dot2)
        bubbleLayout.addView(dot3)

        outerLayout.addView(bubbleLayout)
        chatContainer.addView(outerLayout)

        loadingView = outerLayout

        var step = 0

        dotAnimRunnable = object : Runnable {
            override fun run() {
                step = (step + 1) % 3

                val dots = listOf(dot1, dot2, dot3)

                dots.forEachIndexed { index, dot ->
                    dot.alpha = if (index == step) 1f else 0.3f
                }

                handler.postDelayed(this, 400)
            }
        }

        handler.post(dotAnimRunnable!!)
    }

    private fun createDot(size: Int): View {
        return View(this).apply {
            val params = LinearLayout.LayoutParams(size, size).apply {
                setMargins(2.dpToPx(), 0, 2.dpToPx(), 0)
            }

            layoutParams = params

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFFFFFFF.toInt())
            }

            background = bg
        }
    }

    private fun hideLoadingBubble() {
        dotAnimRunnable?.let {
            handler.removeCallbacks(it)
        }

        loadingView?.let {
            chatContainer.removeView(it)
        }

        loadingView = null
        dotAnimRunnable = null
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun sendMessageToServer(userMessage: String) {
        val token = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .getString("auth_token", null)

        if (token.isNullOrBlank()) {
            hideLoadingBubble()
            addAiMessage("로그인이 필요합니다.")
            scrollToBottom()
            return
        }

        val json = JSONObject()
        json.put("user_message", userMessage)

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideLoadingBubble()
                    addAiMessage("서버 연결에 실패했습니다. 다시 시도해주세요.")
                    scrollToBottom()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    hideLoadingBubble()

                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val aiResponse = jsonResponse.getString("ai_response")
                            addAiMessage(aiResponse)
                        } catch (e: Exception) {
                            addAiMessage("응답을 처리하는 중 오류가 발생했습니다.")
                        }
                    } else {
                        addAiMessage("오류가 발생했습니다. (${response.code})")
                    }

                    scrollToBottom()
                }
            }
        })
    }

    private fun addUserMessage(message: String, saveToHistory: Boolean = true) {
        if (saveToHistory) {
            ChatHistoryManager.messages.add(
                ChatMessage(
                    isUser = true,
                    message = message
                )
            )
        }

        val messageLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 40)
            }

            gravity = android.view.Gravity.END
        }

        val messageView = TextView(this).apply {
            text = message
            textSize = 12f
            setTextColor(0xFF000000.toInt())
            maxWidth = 750
            setBackgroundResource(R.drawable.chat_bubble_user)
            setPadding(36, 24, 36, 24)
        }

        messageLayout.addView(messageView)
        chatContainer.addView(messageLayout)
    }

    private fun addAiMessage(message: String, saveToHistory: Boolean = true) {
        if (saveToHistory) {
            ChatHistoryManager.messages.add(
                ChatMessage(
                    isUser = false,
                    message = message
                )
            )
        }

        val messageLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 40)
            }

            gravity = android.view.Gravity.START
        }

        val messageView = TextView(this).apply {
            text = message
            textSize = 12f
            setTextColor(0xFF000000.toInt())
            maxWidth = 750
            setBackgroundResource(R.drawable.chat_bubble_ai)
            setPadding(36, 24, 36, 24)
        }

        messageLayout.addView(messageView)
        chatContainer.addView(messageLayout)
    }
}