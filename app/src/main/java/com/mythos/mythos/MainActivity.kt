package com.mythos.mythos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var chatInputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var goLoginButton: Button

    private val chatMessages = mutableListOf<ChatMessage>()
    private var generativeModel: GenerativeModel? = null
    private var chat: Chat? = null
    private var retrievedApiKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // A menudo no es necesario si manejas los insets manualmente
        setContentView(R.layout.activity_main)

        // Inicialización de Vistas
        chatInputEditText = findViewById(R.id.chatinput)
        sendButton = findViewById(R.id.chatsend)
        chatRecyclerView = findViewById(R.id.chat_recycler_view)
        goLoginButton = findViewById(R.id.btnGoLogin)

        // Configuración Inicial
        setupRecyclerView()
        setupApiKeyAndInitializeModel()
        setupClickListeners()
        setupWindowInsets()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatMessages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatRecyclerView.adapter = chatAdapter
    }

    private fun setupApiKeyAndInitializeModel() {
        retrievedApiKey = BuildConfig.GEMINI_API_KEY
        if (retrievedApiKey.isNullOrEmpty() || retrievedApiKey == "YOUR_API_KEY_HERE") {
            Log.e("GeminiAI", "API Key no configurada o inválida.")
            addMessageToChat(ChatMessage("Error: API Key no configurada.", Sender.MODEL))
        } else {
            initializeGenerativeModel("gemini-2.5-flash") // O el modelo que prefieras
        }
    }

    private fun setupClickListeners() {
        goLoginButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java)) // Llévalo al perfil si ya está logueado
        }
        sendButton.setOnClickListener { sendMessage() }
        chatInputEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun initializeGenerativeModel(modelToUse: String) {
        // Limpiamos mensajes anteriores de cualquier sesión previa
        chatMessages.clear()
        chatAdapter.notifyDataSetChanged()

        // Mostramos el estado de carga INMEDIATAMENTE
        addMessageToChat(ChatMessage("Forjando una nueva leyenda...", Sender.MODEL, isLoading = true))

        Log.i("GeminiAI", "Intentando inicializar modelo: $modelToUse")
        try {
            val systemPrompt = """
            Eres un Dungeon Master narrando una historia en español. El jugador es el protagonista, y debes contarle la historia a este a través de la segunda persona.
            Debes contar la historia a través de la perspectiva del jugador, comunicándole lo que puede ver, diferentes cosas de interés en relación a la historia, y debes darle al usuario diferentes posibles caminos qué tomar en la historia, de manera indirecta, es decir, ya implementado en la historia, puedes remarcar caminos, personajes, objetos, etc., que el usuario puede o no decidir inspeccionar, repito, de manera indirecta, implementada en la historia, y sin querer mover al usuario en ninguna decisión en particular, es importante que el usuario sea el que decide las decisiones del personaje y que no decidas nada de lo que hace el usuario sin su consentimiento.
            También guías los eventos del mundo. No reveles que eres una IA. Mantén respuestas claras y breves (3-5 oraciones).
            Comienza la aventura ahora con una introducción breve y una situación inicial para el jugador.
            """.trimIndent()

            generativeModel = GenerativeModel(
                modelName = modelToUse,
                apiKey = retrievedApiKey!!,
                generationConfig = generationConfig {
                    temperature = 0.9f
                    topK = 1
                    topP = 1f
                    maxOutputTokens = 2048
                },
                systemInstruction = content("system") { text(systemPrompt) }
            )

            // Generamos la primera respuesta de la IA en una corutina
            lifecycleScope.launch {
                try {
                    val firstResponse = generativeModel!!.generateContent("Comienza la aventura.")
                    val firstAIMessage = firstResponse.text
                    if (!firstAIMessage.isNullOrBlank()) {
                        Log.i("GeminiAI", "Nueva historia generada: $firstAIMessage")
                        chat = generativeModel!!.startChat(
                            history = listOf(
                                content(role = "user") { text("Comienza la aventura.") },
                                content(role = "model") { text(firstAIMessage) }
                            )
                        )
                        // Actualizamos el mensaje de "cargando" con la historia real
                        addMessageToChat(ChatMessage(firstAIMessage, Sender.MODEL), true)
                    } else {
                        throw IllegalStateException("La respuesta inicial de la IA fue nula o vacía.")
                    }
                } catch (e: Exception) {
                    Log.e("GeminiAI", "Error al generar la historia inicial", e)
                    addMessageToChat(ChatMessage("Error al contactar al Oráculo. Intenta de nuevo.", Sender.MODEL), true)
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiAI", "Excepción al inicializar GenerativeModel", e)
            addMessageToChat(ChatMessage("Error configurando IA: ${e.localizedMessage}", Sender.MODEL), true)
            generativeModel = null
            chat = null
        }
    }

    private fun sendMessage() {
        val prompt = chatInputEditText.text.toString().trim()
        if (prompt.isEmpty()) return

        addMessageToChat(ChatMessage(prompt, Sender.USER))
        chatInputEditText.text.clear()

        if (chat == null) {
            addMessageToChat(ChatMessage("Error: La IA no está lista. Reinicia la aventura.", Sender.MODEL))
            return
        }

        addMessageToChat(ChatMessage("...", Sender.MODEL, isLoading = true))

        lifecycleScope.launch {
            try {
                val response = chat!!.sendMessage(prompt)
                response.text?.let { aiResponse ->
                    addMessageToChat(ChatMessage(aiResponse, Sender.MODEL), true)
                } ?: addMessageToChat(ChatMessage("Respuesta vacía recibida.", Sender.MODEL), true)
            } catch (e: Exception) {
                Log.e("GeminiAI", "Error generando contenido", e)
                addMessageToChat(ChatMessage("Error: ${e.message}", Sender.MODEL), true)
            }
        }
    }

    // ----- NUEVA Y ÚNICA FUNCIÓN PARA AÑADIR/ACTUALIZAR MENSAJES -----
    private fun addMessageToChat(message: ChatMessage, isUpdate: Boolean = false) {
        runOnUiThread {
            if (isUpdate && chatMessages.isNotEmpty()) {
                chatAdapter.updateLastMessage(message)
            } else {
                chatAdapter.addMessage(message)
            }
            if (chatAdapter.itemCount > 0) {
                chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
