package com.mythos.mythos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Vistas de la actividad
    private lateinit var tvUserEmail: TextView
    private lateinit var etUsername: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Vinculación de Vistas
        tvUserEmail = findViewById(R.id.tvUserEmail)
        etUsername = findViewById(R.id.etUsername)
        val btnUpdateUsername: Button = findViewById(R.id.btnUpdateUsername)
        val btnResetPassword: Button = findViewById(R.id.btnResetPassword)
        val btnLogout: Button = findViewById(R.id.btnLogout)
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        val btnDelete: Button = findViewById(R.id.btnDelete) // Botón de eliminar

        // Listeners
        btnBack.setOnClickListener { finish() }
        btnLogout.setOnClickListener { signOut() }
        btnUpdateUsername.setOnClickListener { updateUserProfile() }
        btnResetPassword.setOnClickListener { sendPasswordReset() }
        btnDelete.setOnClickListener { showDeleteConfirmationDialog() } // Llama al diálogo

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToHomePage()
            return
        }

        tvUserEmail.text = currentUser.email ?: "Email no disponible"

        db.collection("usuarios").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val username = document?.getString("username") ?: "Aventurero"
                etUsername.setText(username)
            }
            .addOnFailureListener {
                etUsername.setText("Aventurero")
                Toast.makeText(this, "Error al cargar el nombre de usuario.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserProfile() {
        val newUsername = etUsername.text.toString().trim()
        if (newUsername.length < 3) {
            Toast.makeText(this, "El nombre de usuario debe tener al menos 3 caracteres.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(userId)
            .update("username", newUsername)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Nombre de usuario actualizado!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun sendPasswordReset() {
        val email = auth.currentUser?.email
        if (email == null) {
            Toast.makeText(this, "No se encontró un email asociado a la cuenta.", Toast.LENGTH_LONG).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Enlace para restablecer contraseña enviado a $email.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Error al enviar el email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Muestra un diálogo de alerta para confirmar la eliminación de la cuenta.
     */
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("¿Eliminar tu cuenta?")
            .setMessage("Esta acción es irreversible. Se borrarán tu perfil y todas tus aventuras guardadas. ¿Estás seguro?")
            .setPositiveButton("Sí, Eliminar") { _, _ ->
                // Solo si el usuario confirma, se procede a borrar
                deleteAccount()
            }
            .setNegativeButton("Cancelar", null) // 'null' simplemente cierra el diálogo
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    /**
     * Lógica para eliminar la cuenta y los datos asociados.
     */
    private fun deleteAccount() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No hay sesión activa para eliminar.", Toast.LENGTH_SHORT).show()
            goToHomePage()
            return
        }

        // Paso 1: Borrar el documento de usuario en Firestore
        db.collection("usuarios").document(currentUser.uid).delete()
            .addOnSuccessListener {
                Log.d("DeleteAccount", "Datos de Firestore eliminados.")
                // Paso 2: Borrar la cuenta de Firebase Authentication
                currentUser.delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Cuenta eliminada permanentemente.", Toast.LENGTH_LONG).show()
                            // Paso 3: Redirigir y limpiar todo
                            goToHomePage()
                        } else {
                            // Este error puede pasar si la sesión es muy vieja (requiere re-autenticación)
                            Log.e("DeleteAccount", "Error al eliminar la cuenta de Auth.", task.exception)
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DeleteAccount", "Error al eliminar datos de Firestore.", e)
                Toast.makeText(this, "Error al eliminar datos de la cuenta: ${e.message}", Toast.LENGTH_LONG).show()
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
}
