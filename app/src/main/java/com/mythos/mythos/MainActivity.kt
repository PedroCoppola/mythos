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
        setContentView(R.layout.activity_main)

        // 1. Configuración de la UI (es lo primero, siempre debe funcionar)
        setupUI()

        // 2. Lógica Principal: Iniciar el modelo de IA
        startAdventure()
    }

    private fun setupUI() {
        // Inicialización de Vistas
        chatInputEditText = findViewById(R.id.chatinput)
        sendButton = findViewById(R.id.chatsend)
        chatRecyclerView = findViewById(R.id.chat_recycler_view)
        goLoginButton = findViewById(R.id.btnGoLogin)

        // Configuración del RecyclerView
        chatAdapter = ChatAdapter(chatMessages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatRecyclerView.adapter = chatAdapter

        goLoginButton.setOnClickListener {
            // 1. Creamos una intención explícita para ir al Perfil.
            val intent = Intent(this, ProfileActivity::class.java)

            // 2. AÑADIMOS UNA FLAG MÁGICA:
            // Esto le dice al sistema: "Si ya hay una instancia de ProfileActivity
            // en la pila, simplemente tráela al frente en lugar de crear una nueva".
            // Esto previene que se apilen múltiples pantallas de perfil.
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT

            // 3. Lanzamos la actividad.
            startActivity(intent)
        }
        sendButton.setOnClickListener { sendMessage() }
        chatInputEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                return@setOnEditorActionListener true
            }
            false
        }

        // Configuración de los Insets (para el teclado)
        setupWindowInsets()
    }

    private fun startAdventure() {
        // ----- PASO 1: VERIFICAR LA API KEY -----
        retrievedApiKey = BuildConfig.GEMINI_API_KEY
        if (retrievedApiKey.isNullOrEmpty() || retrievedApiKey == "YOUR_API_KEY_HERE") {
            Log.e("GeminiAI", "API Key no configurada o inválida.")
            // Limpiamos la lista y añadimos un ÚNICO mensaje de error.
            runOnUiThread {
                chatMessages.clear()
                chatAdapter.notifyDataSetChanged()
                addMessageToChat(ChatMessage("Error: La API Key no está configurada en el proyecto.", Sender.MODEL))
            }
            return // Detenemos la ejecución aquí. La UI está lista pero la IA no continuará.
        }

        // ----- PASO 2: DECIDIR EL TIPO DE PROMPT -----
        val gameMode = intent.getStringExtra("GAME_MODE")
        val systemPrompt = if (gameMode == "CUSTOM") {
            // Partida personalizada
            val context = intent.getStringExtra("GAME_CONTEXT") ?: "un mundo misterioso"
            val character = intent.getStringExtra("GAME_CHARACTER") ?: "un aventurero sin nombre"
            val toneValue = intent.getIntExtra("GAME_TONE", 50)
            val dialogueValue = intent.getIntExtra("GAME_DIALOGUE", 50)
            buildCustomPrompt(context, character, toneValue, dialogueValue)
        } else {
            // Partida rápida/aleatoria por defecto
            buildDefaultPrompt()
        }

        // ----- PASO 3: INICIALIZAR EL MODELO CON EL PROMPT CORRECTO -----
        initializeGenerativeModel("gemini-2.5-flash", systemPrompt)
    }

    private fun initializeGenerativeModel(modelToUse: String, systemPrompt: String) {
        // 1. PREPARACIÓN DE LA UI PARA LA CARGA
        // Este bloque ahora es el único responsable de limpiar y preparar la lista.
        runOnUiThread {
            chatMessages.clear()
            chatAdapter.notifyDataSetChanged()
            addMessageToChat(ChatMessage("Forjando una nueva leyenda...", Sender.MODEL, isLoading = true))
        }

        Log.i("GeminiAI", "Intentando inicializar modelo: $modelToUse")
        try {
            // 2. INICIALIZACIÓN DEL MODELO
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

            // 3. GENERACIÓN DE LA PRIMERA RESPUESTA
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
                        // 4. ACTUALIZACIÓN SEGURA con el resultado
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


    // --- El resto de las funciones (buildDefaultPrompt, buildCustomPrompt, sendMessage, addMessageToChat, setupWindowInsets) permanecen exactamente iguales ---

    private fun buildDefaultPrompt(): String {
        return """
        Eres un Dungeon Master narrando una historia en español. El jugador es el protagonista, y debes contarle la historia a este a través de la segunda persona.
        No reveles que eres una IA. Mantén respuestas claras y breves (3-5 oraciones).
        Comienza la aventura ahora con una introducción breve y una situación inicial para el jugador, inventando un escenario original cada vez.
        """.trimIndent()
    }

    private fun buildCustomPrompt(context: String, character: String, toneValue: Int, dialogueValue: Int): String {
        val toneDescription = when {
            toneValue < 33 -> "profundamente serio y dramático"
            toneValue > 66 -> "muy cómico, ligero y hasta absurdo"
            else -> "balanceado, con momentos serios y toques de humor"
        }

        val dialogueDescription = when {
            dialogueValue < 33 -> "casi puramente narrativo, con muy pocos diálogos. Enfócate en las descripciones y acciones"
            dialogueValue > 66 -> "muy conversacional. La historia debe avanzar principalmente a través de diálogos con otros personajes"
            else -> "balanceado entre narrativa y diálogo"
        }

        return """
        Eres un Dungeon Master narrando una historia en español. Sigue estas reglas ESTRICTAMENTE:

        1.  **Rol del Jugador:** El jugador es el protagonista. Nárra la historia en segunda persona (ej: "Tú ves...", "Sientes..."). El jugador es quien toma TODAS las decisiones. No asumas ninguna acción por él.

        2.  **Contexto del Mundo:** La historia ocurre aquí: "${if (context.isNotBlank()) context else "un mundo de fantasía genérico"}".

        3.  **Identidad del Protagonista:** El jugador es: "${if (character.isNotBlank()) character else "un aventurero anónimo"}". Debes reflejar esta identidad en la narración.

        4.  **Tono de la Historia:** El tono debe ser ${toneDescription}.

        5.  **Estilo Narrativo:** El estilo debe ser ${dialogueDescription}.

        6.  **Formato:** Mantén respuestas claras y breves (3-5 oraciones). No reveles que eres una IA.

        Comienza la aventura ahora con una introducción breve y una situación inicial para el jugador, respetando todos los puntos anteriores.
        """.trimIndent()
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

    private fun addMessageToChat(message: ChatMessage, isUpdate: Boolean = false) {
        runOnUiThread {
            if (isUpdate && chatMessages.isNotEmpty()) {
                chatAdapter.updateLastMessage(message)
            } else {
                // Previene añadir duplicados si la lista ya tiene un mensaje de carga.
                if (chatMessages.none { it.isLoading }) {
                    chatAdapter.addMessage(message)
                }
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
