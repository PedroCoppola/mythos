package com.mythos.mythos

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isEmpty
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.ai.client.generativeai.type.content // Required for Chat
import com.google.ai.client.generativeai.Chat // Required for Chat
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var chatInputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()

    private lateinit var generativeModel: GenerativeModel
    private var chat: Chat? = null // Store the ongoing chat session

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        chatInputEditText = findViewById(R.id.chatinput)
        sendButton = findViewById(R.id.chatsend)
        chatRecyclerView = findViewById(R.id.chat_recycler_view)

        setupRecyclerView()
        initializeGenerativeModel()

        sendButton.setOnClickListener {
            sendMessage()
        }

        // Optional: Send message when "Enter" is pressed on the keyboard
        chatInputEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                return@setOnEditorActionListener true
            }
            false
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Adjust padding for the main container, but input area handles its own
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // No bottom padding for main

            // Apply bottom padding to the input area container to avoid overlap with system navigation
            val inputArea = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.input_area_container)
            inputArea.setPadding(0,0,0, systemBars.bottom)

            insets
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatMessages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // New messages appear at the bottom
        }
        chatRecyclerView.adapter = chatAdapter
    }

    private fun initializeGenerativeModel() {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY_HERE") {
            Log.e("GeminiAI", "API Key is not set. Please set it in local.properties.")
            addMessageToChat("Error: API Key not configured.", Sender.MODEL)
            return
        }

        generativeModel = GenerativeModel(
            modelName = "gemini-2.0-flash", // Or your preferred model
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 1
                topP = 1f
                maxOutputTokens = 2048
                // Consider adding stopSequences if needed for chat
            }
        )

        // Initialize the chat session
        chat = generativeModel.startChat(
            // Optional: Provide chat history if you want to resume a conversation
            // history = listOf(
            //    content(role = "user") { text("Hello, I have a question about Android development.") },
            //    content(role = "model") { text("Great, what would you like to know?") }
            // )
        )
        addMessageToChat("Hello! How can I help you today?", Sender.MODEL) // Initial greeting from AI
    }

    private fun sendMessage() {
        val prompt = chatInputEditText.text.toString().trim()
        if (prompt.isNotEmpty()) {
            // Add user message to UI
            addMessageToChat(prompt, Sender.USER)
            chatInputEditText.text.clear()

            // Add a loading indicator for the AI response
            val loadingMessage = ChatMessage("...", Sender.MODEL, isLoading = true)
            chatAdapter.addMessage(loadingMessage)
            chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)


            // Send message to the model
            lifecycleScope.launch {
                try {
                    val currentChat = chat ?: run {
                        Log.e("GeminiAI", "Chat session not initialized.")
                        chatAdapter.updateLastMessage(ChatMessage("Error: Chat not initialized.", Sender.MODEL))
                        return@launch
                    }
                    // Send the message and get the response
                    val response = currentChat.sendMessage(prompt) // Use chat.sendMessage

                    // Update the loading message with the actual response
                    response.text?.let { aiResponse ->
                        chatAdapter.updateLastMessage(ChatMessage(aiResponse, Sender.MODEL))
                    } ?: run {
                        chatAdapter.updateLastMessage(ChatMessage("Received an empty response.", Sender.MODEL))
                    }
                    Log.i("GeminiAI", "Response: ${response.text}")

                } catch (e: Exception) {
                    Log.e("GeminiAI", "Error generating content", e)
                    chatAdapter.updateLastMessage(ChatMessage("Error: ${e.localizedMessage}", Sender.MODEL))
                } finally {
                    chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
        }
    }

    private fun addMessageToChat(text: String, sender: Sender) {
        val message = ChatMessage(text, sender)
        chatAdapter.addMessage(message)
        chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1) // Scroll to the new message
    }
}
