package com.mythos.mythos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.json.Json
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // ¡ASEGÚRATE DE AÑADIR ESTE IMPORT!

class HomePageActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Vistas
    private lateinit var rvGameSessions: RecyclerView
    private lateinit var fabNewGame: FloatingActionButton
    private lateinit var tvEmptyState: TextView
    private lateinit var layoutNotLoggedIn: LinearLayout
    private lateinit var btnProfile: CardView

    private lateinit var gameSessionAdapter: GameSessionAdapter

    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instancia la splash antes del setContentView()
        installSplashScreen()


        // --- PASO 2: Continuar con la creación normal ---
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Vinculación de Vistas
        rvGameSessions = findViewById(R.id.rvGameSessions)
        fabNewGame = findViewById(R.id.fabNewGame)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        layoutNotLoggedIn = findViewById(R.id.layoutNotLoggedIn)
        btnProfile = findViewById(R.id.btnProfile)
        findViewById<Button>(R.id.btnGoToRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        setupRecyclerView()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        // Usamos onStart() que se llama siempre que la actividad se hace visible.
        // Esto soluciona el problema de no refrescar después del registro.
        checkUserStatus()
    }

    private fun checkUserStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // --- ESTADO LOGUEADO ---
            Log.d("HomePage", "Usuario logueado: ${currentUser.uid}")
            layoutNotLoggedIn.visibility = View.GONE
            fabNewGame.visibility = View.VISIBLE
            loadGameSessions(currentUser.uid)
        } else {
            // --- ESTADO NO LOGUEADO ---
            Log.d("HomePage", "Usuario no logueado.")
            rvGameSessions.visibility = View.GONE
            tvEmptyState.visibility = View.GONE
            fabNewGame.visibility = View.GONE
            layoutNotLoggedIn.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        gameSessionAdapter = GameSessionAdapter(
            mutableListOf(),
            onItemClick = { session ->
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("GAME_ID_TO_LOAD", session.metadata.gameId)
                startActivity(intent)
            },
            onDeleteClick = { session ->
                showDeleteConfirmationDialog(session)
            }
        )
        rvGameSessions.layoutManager = LinearLayoutManager(this)
        rvGameSessions.adapter = gameSessionAdapter
    }

    private fun setupListeners() {
        fabNewGame.setOnClickListener {
            startActivity(Intent(this, CreateGameActivity::class.java))
        }

        btnProfile.setOnClickListener {
            if (auth.currentUser != null) {
                startActivity(Intent(this, ProfileActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
    }

    private fun loadGameSessions(userId: String) {
        db.collection("game_sessions")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("HomePage", "Error al escuchar cambios.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val sessions = snapshots.documents.mapNotNull { doc ->
                        try {
                            doc.getString("sessionJson")?.let { json.decodeFromString<GameSession>(it) }
                        } catch (ex: Exception) {
                            Log.e("HomePage", "Error al parsear GameSession: ${doc.id}", ex)
                            null
                        }
                    }.sortedByDescending { it.metadata.lastUpdated }

                    gameSessionAdapter.updateSessions(sessions)
                    tvEmptyState.visibility = View.GONE
                    rvGameSessions.visibility = View.VISIBLE
                } else {
                    gameSessionAdapter.updateSessions(emptyList())
                    rvGameSessions.visibility = View.GONE
                    tvEmptyState.visibility = View.VISIBLE
                }
            }
    }

    private fun showDeleteConfirmationDialog(session: GameSession) {
        AlertDialog.Builder(this, R.style.AlertDialogTheme) // Aplicamos tema
            .setTitle("Confirmar Borrado")
            .setMessage("¿Estás seguro de que quieres borrar la aventura '${session.metadata.gameName}'?")
            .setPositiveButton("Borrar") { _, _ -> deleteGameSession(session) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteGameSession(session: GameSession) {
        db.collection("game_sessions").document(session.metadata.gameId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Historia '${session.metadata.gameName}' borrada.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al borrar la aventura.", Toast.LENGTH_SHORT).show()
            }
    }
}
