package com.mythos.mythos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    // Declaramos las variables de Firebase a nivel de clase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Declaramos las vistas para accederlas más fácil
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializamos Firebase Auth y Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Vinculamos las vistas con sus IDs del XML
        // CAMBIO: Ahora usamos los IDs correctos del XML
        emailEditText = findViewById(R.id.edtEmailRegistro)
        passwordEditText = findViewById(R.id.edtPasswordRegistro)
        val btnRegistrar = findViewById<Button>(R.id.btnRegister)
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        val btnGoToLogin: TextView = findViewById(R.id.btnGoToLogin) // El TextView clickeable

        // Configuramos los listeners de los botones
        btnBack.setOnClickListener {
            finish() // Cierra esta actividad y vuelve a la anterior
        }

        btnGoToLogin.setOnClickListener {
            // Inicia LoginActivity y cierra la actual para que no se apilen
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRegistrar.setOnClickListener {
            // Cuando se presiona "Registrar", llamamos a nuestra función de lógica
            registerUser()
        }
    }

    /**
     * Esta es la función que contiene TODA la lógica para registrar a un usuario
     * usando Firebase Authentication.
     */
    private fun registerUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa el email y la contraseña.", Toast.LENGTH_SHORT).show()
            return
        }

        // Esta es la llamada clave a Firebase Auth para crear un usuario
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // --- REGISTRO EXITOSO ---
                    Log.d("RegisterActivity", "createUserWithEmail:success")
                    Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()

                    // Opcional: Guardar datos adicionales del usuario en Firestore
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val userMap = hashMapOf(
                            "email" to email,
                            "username" to email.substringBefore('@'), // Un nombre de usuario por defecto
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("usuarios").document(userId).set(userMap)
                            .addOnSuccessListener { Log.d("RegisterActivity", "Datos adicionales del usuario guardados.") }
                            .addOnFailureListener { e -> Log.w("RegisterActivity", "Error al guardar datos adicionales.", e) }
                    }

                    // Mandamos al usuario a la HomePage
                    val intent = Intent(this, HomePageActivity::class.java)
                    // Limpiamos la pila de actividades para que no pueda "volver" a la pantalla de registro
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // Cierra la RegisterActivity para que no quede en el historial

                } else {
                    // --- REGISTRO FALLIDO ---
                    Log.w("RegisterActivity", "createUserWithEmail:failure", task.exception)
                    // Mostramos el mensaje de error que nos da Firebase, que es mucho más útil
                    Toast.makeText(this, "Falló el registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
