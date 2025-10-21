package com.mythos.mythos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // --- Vistas ---
    private lateinit var chatInputEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var btnBack: ImageButton
    private lateinit var btnProfile: ImageButton
    private lateinit var tvStoryTitle: TextView

    // --- Lógica y Firebase ---
    private var generativeModel: GenerativeModel? = null
    private var retrievedApiKey: String? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var currentGameSession: GameSession? = null
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private var chat: Chat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        retrievedApiKey = BuildConfig.GEMINI_API_KEY

        setupUI()
        setupListeners()
        startOrLoadAdventure()
    }

    private fun setupUI() {
        btnBack = findViewById(R.id.btnBack)
        btnProfile = findViewById(R.id.btnProfile)
        tvStoryTitle = findViewById(R.id.tvStoryTitle)
        chatInputEditText = findViewById(R.id.chatinput)
        sendButton = findViewById(R.id.chatsend)
        chatRecyclerView = findViewById(R.id.chat_recycler_view)

        chatAdapter = ChatAdapter(mutableListOf())
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        chatRecyclerView.adapter = chatAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnProfile.setOnClickListener {
            val intent = if (auth.currentUser != null) {
                Intent(this, ProfileActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
            startActivity(intent)
        }

        sendButton.setOnClickListener { sendMessage() }
    }

    private fun startOrLoadAdventure() {
        if (retrievedApiKey.isNullOrEmpty() || retrievedApiKey == "YOUR_API_KEY_HERE") {
            lifecycleScope.launch { updateChatUI(listOf(ChatMessage("Error: La API Key no está configurada.", Sender.MODEL))) }
            return
        }

        val gameIdToLoad = intent.getStringExtra("GAME_ID_TO_LOAD")
        if (gameIdToLoad != null) {
            loadExistingGame(gameIdToLoad)
        } else {
            // CORRECCIÓN: Recibir todos los parámetros de CreateGameActivity
            val context = intent.getStringExtra("GAME_CONTEXT") ?: "un mundo de fantasía misterioso"
            val character = intent.getStringExtra("GAME_CHARACTER") ?: "un aventurero sin nombre"
            val toneValue = intent.getIntExtra("GAME_TONE", 50)
            val dialogueValue = intent.getIntExtra("GAME_DIALOGUE", 50)
            startNewAdventure(context, character, toneValue, dialogueValue)
        }
    }

    private fun startNewAdventure(context: String, character: String, toneValue: Int, dialogueValue: Int) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Debes iniciar sesión para crear una historia.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val tempTitle = "Nueva Aventura..."
        tvStoryTitle.text = tempTitle

        currentGameSession = GameSession(
            metadata = GameMetadata(
                gameId = UUID.randomUUID().toString(),
                userId = currentUserId,
                gameName = tempTitle,
                summary = "La aventura está a punto de comenzar.",
                lastUpdated = System.currentTimeMillis(),
                userContext = context,
                userCharacter = character,
                tone = toneValue,
                dialogueStyle = dialogueValue
            ),
            history = mutableListOf()
        )

        val systemPrompt = buildCustomPrompt(context, character, toneValue, dialogueValue)
        initializeAndStart(systemPrompt)
    }

    private fun initializeAndStart(systemPrompt: String) {
        lifecycleScope.launch { updateChatUI(listOf(ChatMessage("Forjando una nueva leyenda...", Sender.MODEL, isLoading = true))) }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                generativeModel = GenerativeModel(
                    modelName = "gemini-2.5-flash", apiKey = retrievedApiKey!!,
                    generationConfig = generationConfig { temperature = 0.9f; topK = 1; topP = 1f; maxOutputTokens = 2048 },
                    systemInstruction = content("system") { text(systemPrompt) }
                )
                chat = generativeModel!!.startChat()

                val firstResponse = chat!!.sendMessage("Comienza la aventura.")
                val firstAIMessage = firstResponse.text?.trim() ?: "El oráculo no responde..."
                val firstMessageObject = ChatMessage(firstAIMessage, Sender.MODEL)
                currentGameSession?.history?.add(firstMessageObject)

                // LÓGICA RECUPERADA: Generar el nombre y resumen iniciales
                val (gameName, summary) = generateInitialMetadata(firstAIMessage)
                currentGameSession?.metadata?.gameName = gameName
                currentGameSession?.metadata?.summary = summary
                currentGameSession?.metadata?.lastUpdated = System.currentTimeMillis()

                withContext(Dispatchers.Main) {
                    tvStoryTitle.text = gameName
                }

                updateChatUI(currentGameSession!!.history)
                saveGameSessionToFirebase()

            } catch (e: Exception) {
                Log.e("GeminiAI_Init", "Error al inicializar la partida", e)
                updateChatUI(listOf(ChatMessage("Error al contactar al Oráculo.", Sender.MODEL)))
            }
        }
    }

    private fun loadExistingGame(gameId: String) {
        db.collection("game_sessions").document(gameId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val sessionJson = document.getString("sessionJson")
                        val loadedSession = sessionJson?.let { json.decodeFromString<GameSession>(it) }
                        if (loadedSession != null) {
                            currentGameSession = loadedSession
                            chatAdapter.updateMessages(currentGameSession!!.history)
                            tvStoryTitle.text = loadedSession.metadata.gameName

                            val metadata = currentGameSession!!.metadata
                            val systemPrompt = buildCustomPrompt(metadata.userContext, metadata.userCharacter, metadata.tone, metadata.dialogueStyle)

                            generativeModel = GenerativeModel(
                                modelName = "gemini-2.5-flash", apiKey = retrievedApiKey!!,
                                generationConfig = generationConfig { temperature = 0.9f; topK = 1; topP = 1f; maxOutputTokens = 2048 },
                                systemInstruction = content("system") { text(systemPrompt) }
                            )

                            val historyForAI = currentGameSession!!.history.map { content(if (it.sender == Sender.USER) "user" else "model") { text(it.text) } }
                            chat = generativeModel!!.startChat(history = historyForAI)
                            Toast.makeText(this, "Aventura '${metadata.gameName}' cargada.", Toast.LENGTH_SHORT).show()

                        } else { throw IllegalStateException("La sesión cargada es nula.") }
                    } catch (e: Exception) {
                        Log.e("LoadGame", "Error al parsear o configurar la partida cargada", e)
                        Toast.makeText(this, "Error al cargar la aventura.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Log.e("LoadGame", "No se encontró el documento de la partida con ID: $gameId")
                    Toast.makeText(this, "No se encontró la aventura.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoadGame", "Error al obtener la partida de Firestore", e)
                Toast.makeText(this, "Error de conexión al cargar la aventura.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun sendMessage() {
        val prompt = chatInputEditText.text.toString().trim()
        if (prompt.isEmpty() || generativeModel == null || chat == null) return

        lifecycleScope.launch(Dispatchers.IO) {
            val userMessage = ChatMessage(prompt, Sender.USER)
            currentGameSession?.history?.add(userMessage)
            val loadingMessage = ChatMessage("...", Sender.MODEL, isLoading = true)
            currentGameSession?.history?.add(loadingMessage)
            updateChatUI(currentGameSession!!.history)
            withContext(Dispatchers.Main) { chatInputEditText.text.clear() }

            try {
                val response = chat!!.sendMessage(prompt)
                val aiResponseText = response.text?.trim() ?: "El oráculo guarda silencio..."
                val aiMessageObject = ChatMessage(aiResponseText, Sender.MODEL)

                if (currentGameSession?.history?.isNotEmpty() == true) {
                    currentGameSession?.history?.removeAt(currentGameSession!!.history.lastIndex)
                }
                currentGameSession?.history?.add(aiMessageObject)
                updateChatUI(currentGameSession!!.history)

                // LÓGICA RECUPERADA: Generar el nuevo resumen
                val newSummary = generateNewSummary()
                currentGameSession?.metadata?.summary = newSummary
                currentGameSession?.metadata?.lastUpdated = System.currentTimeMillis()
                saveGameSessionToFirebase()

            } catch (e: Exception) {
                Log.e("GeminiAI_Send", "Error al enviar mensaje", e)
                if (currentGameSession?.history?.isNotEmpty() == true) {
                    currentGameSession?.history?.removeAt(currentGameSession!!.history.lastIndex)
                }
                currentGameSession?.history?.add(ChatMessage("Error: ${e.message}", Sender.MODEL))
                updateChatUI(currentGameSession!!.history)
            }
        }
    }

    private suspend fun generateInitialMetadata(storyText: String): Pair<String, String> {
        val metadataPrompt = """
            Basado en el siguiente texto: "$storyText"
            Responde a estas dos preguntas en formato `KEY:[VALUE]`:
            1. NAME:[Inventa un nombre épico y corto para esta aventura]
            2. SUMMARY:[Escribe un resumen de una frase sobre la situación inicial]
        """.trimIndent()
        try {
            val response = generativeModel!!.generateContent(metadataPrompt)
            val text = response.text ?: ""
            Log.d("METADATA_DEBUG", "Respuesta para metadatos iniciales: $text")
            val name = "NAME:\\[(.*?)]".toRegex(RegexOption.DOT_MATCHES_ALL).find(text)?.groups?.get(1)?.value?.trim() ?: "Aventura sin Nombre"
            val summary = "SUMMARY:\\[(.*?)]".toRegex(RegexOption.DOT_MATCHES_ALL).find(text)?.groups?.get(1)?.value?.trim() ?: "Sin resumen."
            return Pair(name, summary)
        } catch (e: Exception) {
            Log.e("GeminiAI_Meta", "Falló la generación de metadatos iniciales", e)
            return Pair("Aventura sin Nombre", "Sin resumen.")
        }
    }

    private suspend fun generateNewSummary(): String {
        val historyText = currentGameSession?.history?.joinToString("\n") { "${it.sender}: ${it.text}" } ?: ""
        val summaryPrompt = """
            Considerando la siguiente historia:
            "$historyText"
            Escribe un nuevo resumen actualizado de la aventura en una sola frase, en formato `SUMMARY:[resumen]`.
        """.trimIndent()
        try {
            val response = generativeModel!!.generateContent(summaryPrompt)
            val text = response.text ?: ""
            Log.d("METADATA_DEBUG", "Respuesta para resumen nuevo: $text")
            return "SUMMARY:\\[(.*?)]".toRegex(RegexOption.DOT_MATCHES_ALL).find(text)?.groups?.get(1)?.value?.trim() ?: currentGameSession?.metadata?.summary ?: "Sin resumen."
        } catch (e: Exception) {
            Log.e("GeminiAI_Meta", "Falló la actualización del resumen", e)
            return currentGameSession?.metadata?.summary ?: "Sin resumen."
        }
    }

    private suspend fun updateChatUI(messages: List<ChatMessage>) {
        withContext(Dispatchers.Main) {
            chatAdapter.updateMessages(messages)
            if (chatAdapter.itemCount > 0) {
                chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    private suspend fun saveGameSessionToFirebase() {
        val session = currentGameSession ?: return
        val sessionToSave = session.copy(history = session.history.filter { !it.isLoading }.toMutableList())

        Log.d("FIRESTORE_SAVE", "Intentando guardar la partida: ${sessionToSave.metadata.gameId}")
        try {
            val sessionJsonString = json.encodeToString(GameSession.serializer(), sessionToSave)
            Log.d("FIRESTORE_SAVE", "JSON a subir:\n$sessionJsonString")
            val sessionData = mapOf("sessionJson" to sessionJsonString, "userId" to sessionToSave.metadata.userId)

            db.collection("game_sessions").document(sessionToSave.metadata.gameId)
                .set(sessionData, SetOptions.merge())
                .addOnSuccessListener { Log.d("FIRESTORE_SAVE", "¡ÉXITO!") }
                .addOnFailureListener { e -> Log.e("FIRESTORE_SAVE", "¡FALLO!", e) }
        } catch (e: Exception) {
            Log.e("FIRESTORE_SAVE", "Error CRÍTICO al serializar o guardar.", e)
        }
    }

    private fun buildCustomPrompt(context: String, character: String, toneValue: Int, dialogueValue: Int): String {
        val toneDescription = when {
            toneValue < 33 -> "profundamente serio y dramático"
            toneValue > 66 -> "muy cómico, ligero y hasta absurdo"
            else -> "balanceado, con momentos serios y toques de humor"
        }
        val dialogueDescription = when {
            dialogueValue < 33 -> "casi puramente narrativo, con muy pocos diálogos"
            dialogueValue > 66 -> "muy conversacional, la historia avanza por diálogos"
            else -> "balanceado entre narrativa y diálogo"
        }
        return """
        Eres un Dungeon Master narrando una historia en español. Sigue estas reglas ESTRICTAMENTE:
        1.  **Rol del Jugador:** El jugador es el protagonista. Nárra la historia en segunda persona (ej: "Tú ves..."). El jugador toma TODAS las decisiones. No asumas acciones por él.
        2.  **Contexto:** La historia ocurre aquí: "$context".
        3.  **Protagonista:** El jugador es: "$character".
        4.  **Tono:** El tono debe ser $toneDescription.
        5.  **Estilo:** El estilo debe ser $dialogueDescription.
        6.  **Formato:** Respuestas de 3-5 oraciones. No reveles que eres una IA.
        Comienza la aventura ahora con una introducción breve y una situación inicial, respetando todos los puntos.
        """.trimIndent()
    }
}
