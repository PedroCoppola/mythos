package com.mythos.mythos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvUserEmail: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvGamesCreatedCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Vistas
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvUsername = findViewById(R.id.tvUsername)
        tvGamesCreatedCount = findViewById(R.id.tvGamesCreatedCount)
        val btnLogout: Button = findViewById(R.id.btnLogout)
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        val btnDelete: Button = findViewById(R.id.btnDelete)

        // Listeners
        btnBack.setOnClickListener {
            finish()
        }

        btnLogout.setOnClickListener {
            signOut()
        }

        btnDelete.setOnClickListener {
            deleteAccount()
        }

        loadUserProfile()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Si por alguna razón llega acá sin estar logueado, lo mandamos a la home.
            Toast.makeText(this, "No hay sesión activa.", Toast.LENGTH_SHORT).show()
            goToHomePage()
            return
        }

        // --- Cargar datos del usuario desde Firestore ---
        tvUserEmail.text = currentUser.email ?: "Email no disponible"

        // Cargar nombre de usuario desde la colección "usuarios"
        db.collection("usuarios").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username") ?: "Aventurero"
                    tvUsername.text = username
                } else {
                    tvUsername.text = "Aventurero Anónimo"
                }
            }
            .addOnFailureListener {
                tvUsername.text = "Aventurero"
            }

        // --- Contar las partidas del usuario ---
        db.collection("game_sessions")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                tvGamesCreatedCount.text = documents.size().toString()
            }
            .addOnFailureListener {
                tvGamesCreatedCount.text = "N/A"
            }
    }



    private fun signOut() {
        auth.signOut()
        Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
        goToHomePage()
    }

    private fun goToHomePage() {
        val intent = Intent(this, HomePageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun deleteAccount() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Si por alguna razón llega acá sin estar logueado, lo mandamos a la home.
            Toast.makeText(this, "No hay usuario autenticado.", Toast.LENGTH_SHORT).show()
            goToHomePage()
            return
        }

        db.collection("usuarios").document(currentUser.uid).delete()
            .addOnSuccessListener{
                currentUser.delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()
                        goToHomePage()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al eliminar cuenta: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar datos ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
