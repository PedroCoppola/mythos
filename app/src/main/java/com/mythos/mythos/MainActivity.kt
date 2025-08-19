package com.mythos.mythos

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
// import android.widget.TextView // No usado directamente, puedes quitarlo si no hay otras referencias
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
// import androidx.core.view.isEmpty // No usado directamente
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

    // Cambia lateinit var por un tipo anulable e inicialízalo a null
    private var generativeModel: GenerativeModel? = null
    private var chat: Chat? = null

    // Almacenamos la API key una vez que se recupera
    private var retrievedApiKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        chatInputEditText = findViewById(R.id.chatinput)
        sendButton = findViewById(R.id.chatsend)
        chatRecyclerView = findViewById(R.id.chat_recycler_view)

        // Recuperar API Key
        retrievedApiKey = BuildConfig.GEMINI_API_KEY // Asegúrate que esto funcione y GEMINI_API_KEY esté bien en build.gradle

        if (retrievedApiKey.isNullOrEmpty() || retrievedApiKey == "YOUR_API_KEY_HERE") {
            Log.e("GeminiAI", "API Key no configurada o inválida. Por favor, configúrala en local.properties.")
            // Considera mostrar un error más visible al usuario aquí, si es apropiado
            addMessageToChat("Error: API Key no configurada.", Sender.MODEL)
            // No continuar si no hay API key
        } else {
            // Solo inicializar si la API key es válida
            setupRecyclerView()
            initializeGenerativeModel("gemini-1.5-flash") // Usa el nombre de modelo que te funciona
            // Por ejemplo: "gemini-1.5-flash"
            // ¡ASEGÚRATE DE USAR EL NOMBRE CORRECTO AQUÍ!
        }


        sendButton.setOnClickListener {
            sendMessage()
        }

        chatInputEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                return@setOnEditorActionListener true
            }
            false
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            var imeVisible = false
            var imeHeight = 0

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            }


            // Ajuste de padding para el contenedor principal
            // El padding inferior será la altura del teclado si está visible, o el de las barras del sistema si no.
            val bottomPadding = if (imeVisible) imeHeight else systemBars.bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)

            // El input_area_container está dentro de 'v' (main), por lo que se moverá con el padding de 'v'.
            // No necesitamos aplicarle insets directamente si el 'main' se ajusta correctamente.
            // Si quieres que el input_area_container tenga su propio fondo y se pegue justo encima del teclado,
            // la lógica anterior donde 'main' no tenía padding inferior y 'input_area_container' sí lo recibía era mejor.
            // Vamos a revertir a una lógica más similar a la que tenías, pero usando imeHeight para el input_area.

            // Reset padding para 'main' para que ocupe todo el espacio menos las barras superiores
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)

            // Aplicar padding al input_area_container para que no se solape con la barra de navegación o el teclado
            val inputArea = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.input_area_container)
            val inputAreaBottomPadding = if (imeVisible && imeHeight > systemBars.bottom) {
                imeHeight // Si el teclado es más alto que la barra de navegación, usa la altura del teclado
            } else {
                systemBars.bottom // Sino, usa la altura de la barra de navegación
            }
            inputArea.setPadding(inputArea.paddingLeft, inputArea.paddingTop, inputArea.paddingRight, inputAreaBottomPadding)


            // Esto es crucial para el RecyclerView: ajusta su padding inferior
            // para que su último elemento sea visible sobre el área de input.
            // Necesitamos la altura del input_area_container.
            inputArea.post { // Usamos post para asegurar que las dimensiones del inputArea estén calculadas
                chatRecyclerView.setPadding(0,0,0, inputArea.height)
            }


            insets
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatMessages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatRecyclerView.adapter = chatAdapter
    }

    private fun initializeGenerativeModel(modelToUse: String) { // modelToUse se pasa como argumento
        val currentApiKey = retrievedApiKey // Usar la API key ya recuperada
        if (currentApiKey.isNullOrEmpty()) { // Doble chequeo, aunque ya se hizo en onCreate
            Log.e("GeminiAI", "API Key es nula o vacía durante la inicialización del modelo.")
            addMessageToChat("Error: API Key no disponible para iniciar el modelo.", Sender.MODEL)
            return
        }

        Log.i("GeminiAI", "Intentando inicializar modelo: $modelToUse con API Key: $currentApiKey")

        try {
            val systemPrompt = """
            Eres un Dungeon Master narrando una historia en español. El jugador es el protagonista, y debes contarle la historia a este a través de la segunda persona.
            Debes contar la historia a través de la perspectiva del jugador, comunicándole lo que puede ver, diferentes cosas de interés en relación a la historia, y debes darle al usuario diferentes posibles caminos qué tomar en la historia, de manera indirecta, es decir, ya implementado en la historia, puedes remarcar caminos, personajes, objetos, etc., que el usuario puede o no decidir inspeccionar, repito, de manera indirecta, implementada en la historia, y sin querer mover al usuario en ninguna decisión en particular, es importante que el usuario sea el que decide las decisiones del personaje y que no decidas nada de lo que hace el usuario sin su consentimiento.
            También guías los eventos del mundo. No reveles que eres una IA. Mantén respuestas claras y breves (3-5 oraciones).
            Comienza la aventura ahora con una introducción breve y una situación inicial para el jugador.
            """.trimIndent()

            val initialUserMessageForDM = "System Prompt (para el DM): $systemPrompt\n\nAhora, por favor, comienza la historia para el jugador."
            val firstAIMessage = "Te despiertas en un bosque oscuro y brumoso. Apenas puedes ver a un metro de distancia. Un sendero estrecho se adentra entre los árboles retorcidos hacia el norte, y escuchas el leve murmullo de agua hacia el este. ¿Qué haces?" // Personaliza esto

            generativeModel = GenerativeModel(
                modelName = modelToUse,
                apiKey = currentApiKey,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    topK = 1
                    topP = 1f
                    maxOutputTokens = 2048
                }
                // Si tu SDK y modelo soportan `systemInstruction` directamente, es preferible:
                // systemInstruction = content("system") { text(systemPrompt) }
            )

            // Si `systemInstruction` no se usa arriba, inyecta el prompt en el historial
            // Si usaste `systemInstruction`, el historial puede empezar vacío o solo con el primer mensaje del modelo.
            chat = generativeModel?.startChat(
                history = listOf(
                    content(role = "user") { text(initialUserMessageForDM) }, // Solo si no usas systemInstruction
                    content(role = "model") { text(firstAIMessage) }
                )
            )

            if (chat != null) {
                Log.i("GeminiAI", "Modelo '$modelToUse' inicializado y chat comenzado con prompt de DM.")
                chatMessages.clear()
                chatMessages.add(ChatMessage(firstAIMessage, Sender.MODEL))
                chatAdapter.notifyDataSetChanged()
                if (chatAdapter.itemCount > 0) {
                    chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }
            } else {
                Log.e("GeminiAI", "Falló la inicialización del modelo o el inicio del chat para '$modelToUse'.")
                addMessageToChat("Error: No se pudo iniciar la sesión de IA con '$modelToUse'.", Sender.MODEL)
            }

        } catch (e: Exception) {
            Log.e("GeminiAI", "Excepción al inicializar GenerativeModel con '$modelToUse'", e)
            addMessageToChat("Error configurando IA ($modelToUse): ${e.localizedMessage}", Sender.MODEL)
            generativeModel = null // Ahora esto es válido porque generativeModel es GenerativeModel?
            chat = null
        }
    }

    private fun sendMessage() {
        val prompt = chatInputEditText.text.toString().trim()
        if (prompt.isNotEmpty()) {
            addMessageToChat(prompt, Sender.USER)
            chatInputEditText.text.clear()

            if (generativeModel == null || chat == null) {
                Log.e("GeminiAI", "El modelo o el chat no están inicializados. No se puede enviar mensaje.")
                addMessageToChat("Error: La IA no está lista. Intenta reiniciar.", Sender.MODEL)
                return
            }

            val loadingMessage = ChatMessage("...", Sender.MODEL, isLoading = true)
            // Asegurarse que el adapter no esté vacío antes de llamar a addMessage si setupRecyclerView no se llamó
            if (::chatAdapter.isInitialized) {
                chatAdapter.addMessage(loadingMessage)
                chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            } else {
                Log.e("GeminiAI", "ChatAdapter no inicializado en sendMessage")
                // Manejar este caso, quizás mostrando un error o no haciendo nada
                return
            }


            lifecycleScope.launch {
                try {
                    val response = chat!!.sendMessage(prompt) // Usamos !! porque chequeamos chat != null arriba
                    response.text?.let { aiResponse ->
                        chatAdapter.updateLastMessage(ChatMessage(aiResponse, Sender.MODEL))
                    } ?: run {
                        chatAdapter.updateLastMessage(ChatMessage("Respuesta vacía recibida.", Sender.MODEL))
                    }
                    Log.i("GeminiAI", "Respuesta: ${response.text}")
                } catch (e: Exception) {
                    Log.e("GeminiAI", "Error generando contenido", e)
                    chatAdapter.updateLastMessage(ChatMessage("Error: ${e.message}", Sender.MODEL))
                } finally {
                    // Solo scrollear si el adapter tiene items
                    if (chatAdapter.itemCount > 0) {
                        chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                    }
                }
            }
        }
    }

    private fun addMessageToChat(text: String, sender: Sender) {
        // Solo agregar mensajes si el adapter está inicializado
        if (!::chatAdapter.isInitialized) {
            Log.w("GeminiAI", "addMessageToChat llamado antes que chatAdapter esté inicializado.")
            // Podrías encolar mensajes tempranos si es necesario, o simplemente ignorarlos/loguearlos
            return
        }
        runOnUiThread {
            val message = ChatMessage(text, sender)
            chatAdapter.addMessage(message)
            if (chatAdapter.itemCount > 0) {
                chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }
}
