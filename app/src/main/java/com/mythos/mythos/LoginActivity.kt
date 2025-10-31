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

class LoginActivity : AppCompatActivity() {

    // Declaramos la variable de Firebase Auth a nivel de clase
    private lateinit var auth: FirebaseAuth

    // Declaramos las vistas para accederlas más fácil
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La línea "enableEdgeToEdge()" puede dar problemas a veces, la comentamos
        // si el layout se ve raro.
        // enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Inicializamos Firebase Auth
        auth = FirebaseAuth.getInstance()

        // ---  VERIFICACIÓN DE SESIÓN INICIAL ---
        // Si el usuario YA está logueado, lo mandamos directo a la HomePage sin que vea el login.
        if (auth.currentUser != null) {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish() // Cierra LoginActivity para que no pueda volver atrás
            return // Detiene la ejecución del onCreate para no hacer trabajo de más
        }

        // --- Vinculación de Vistas con los IDs CORRECTOS del XML ---
        // CORRECCIÓN: Usamos los IDs del layout que me pasaste
        emailEditText = findViewById(R.id.edtEmailLogin)
        passwordEditText = findViewById(R.id.edtPasswordLogin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoToRegister = findViewById<TextView>(R.id.btnGoToRegister)
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // --- Listeners de los botones ---
        btnGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish() // Cierra el login para no apilarlo
        }

        btnBack.setOnClickListener {
            finish() // Cierra la actividad y vuelve a la anterior
        }

        btnLogin.setOnClickListener {
            // Cuando se presiona "Ingresar", llamamos a nuestra función de lógica
            loginUser()
        }

        tvForgotPassword.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Ingresá tu email para recuperar la contraseña.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Te enviamos un mail para recuperar tu contraseña. Revisa tu bandeja de spam.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error al enviar el mail: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        Log.e("LoginActivity", "Error al enviar mail de recuperación", task.exception)
                    }
                }
        }

    }




    /**
     * Esta función contiene TODA la lógica para iniciar sesión
     * usando Firebase Authentication.
     */
    private fun loginUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa el email y la contraseña.", Toast.LENGTH_SHORT).show()
            return
        }

        // Esta es la llamada clave a Firebase Auth para iniciar sesión
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // --- LOGIN EXITOSO ---
                    Log.d("LoginActivity", "signInWithEmail:success")
                    Toast.makeText(this, "¡Bienvenido de vuelta!", Toast.LENGTH_SHORT).show()

                    // Mandamos al usuario a la HomePage
                    val intent = Intent(this, HomePageActivity::class.java)
                    // Limpiamos la pila de actividades para que no pueda "volver" a la pantalla de login
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // Cierra LoginActivity

                } else {
                    // --- LOGIN FALLIDO ---
                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                    // Mostramos el mensaje de error que nos da Firebase
                    Toast.makeText(this, "Error de autenticación: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
