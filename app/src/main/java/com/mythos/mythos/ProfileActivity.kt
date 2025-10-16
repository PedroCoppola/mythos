package com.mythos.mythos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    // Este es el "receptor" moderno para los datos que vuelven de CreateGameActivity.
    // Está diseñado para esperar un resultado.
    private val createGameLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Recibimos los datos de la partida personalizada
            val data: Intent? = result.data

            // Creamos un nuevo Intent para MainActivity, pasando TODOS los datos que recibimos
            val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
                // Usamos 'putExtras' para copiar todo el paquete de datos de una vez
                data?.extras?.let { putExtras(it) }
            }
            startActivity(mainActivityIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        db = FirebaseFirestore.getInstance()

        // Referencias a los elementos de la UI
        val tvUsername: TextView = findViewById(R.id.tvUsername)
        val tvCreationDate: TextView = findViewById(R.id.tvCreationDate)
        val btnStartStory: Button = findViewById(R.id.btnStartStory)
        val btnLogout: Button = findViewById(R.id.btnLogout)
        val btnGoToCreateGame: Button = findViewById(R.id.btnGoToCreateGame)

        // 1. Obtener los datos de la sesión guardada
        val sharedPref = getSharedPreferences("MiAppPrefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("usuario", null)

        // Si por alguna razón no hay usuario, no deberíamos estar aquí. Volvemos al login.
        if (username == null) {
            goLogin()
            return
        }

        // 2. Mostrar datos y cargar fecha desde Firestore
        tvUsername.text = username
        loadUserCreationDate(username)

        // 3. Configurar los botones

        // --- ¡¡¡ ESTE ES EL CAMBIO CLAVE Y CORREGIDO !!! ---
        btnGoToCreateGame.setOnClickListener {
            val intent = Intent(this, CreateGameActivity::class.java)
            // Usamos el launcher que está diseñado para esperar un resultado.
            createGameLauncher.launch(intent)
        }

        // Botón para iniciar una partida RÁPIDA (sin parámetros)
        btnStartStory.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Botón para cerrar sesión
        btnLogout.setOnClickListener {
            // Borrar los datos de la sesión guardada en el dispositivo
            sharedPref.edit().remove("usuario").apply()

            // Mostrar un mensaje de confirmación
            Toast.makeText(this, "Has cerrado sesión", Toast.LENGTH_SHORT).show()

            // Navegar de vuelta a la pantalla de login
            goLogin()
        }
    }

    private fun loadUserCreationDate(username: String) {
        val tvCreationDate: TextView = findViewById(R.id.tvCreationDate)

        db.collection("usuarios").document(username).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val timestamp = document.getTimestamp("fechaCreacion")
                    if (timestamp != null) {
                        val date: Date = timestamp.toDate()
                        val formatter = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
                        tvCreationDate.text = "Miembro desde el ${formatter.format(date)}"
                    } else {
                        tvCreationDate.text = "Fecha de registro no disponible"
                    }
                }
            }
            .addOnFailureListener {
                tvCreationDate.text = "No se pudo cargar la fecha"
            }
    }

    private fun goLogin() {
        // Crea una intención para ir a LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        // Estas flags son CRUCIALES: borran el historial de pantallas ("back stack")
        // para que el usuario no pueda volver a la pantalla de perfil con el botón "atrás"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // <-- ¡¡¡ESTA LÍNEA ES LA QUE FALTABA Y SOLUCIONA TODO!!!
    }
}
