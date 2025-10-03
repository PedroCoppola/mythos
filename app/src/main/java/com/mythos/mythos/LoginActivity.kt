package com.mythos.mythos

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login_activity)

        val sharedPref = getSharedPreferences("MiAppPrefs", Context.MODE_PRIVATE)
        val usuarioGuardado = sharedPref.getString("usuario", null)
        if (usuarioGuardado != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val edtUsuario = findViewById<EditText>(R.id.edtUsuario)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnIrRegistro = findViewById<Button>(R.id.btnGoRegister)
        val btnBack = findViewById<Button>(R.id.btnBack)

        db = FirebaseFirestore.getInstance()

        btnIrRegistro.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnLogin.setOnClickListener {
            val usuario = edtUsuario.text.toString()
            val contraseña = edtPassword.text.toString()

            if (usuario.isEmpty() || contraseña.isEmpty()){
                Toast.makeText(this, "Por favor completar los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("usuarios").document(usuario).get()
                .addOnSuccessListener { document ->
                    if (document.exists()){
                        val passwordDB = document.getString("contraseña")
                        if (contraseña == passwordDB){
                            val editor = sharedPref.edit()
                            editor.putString("usuario", usuario)
                            editor.apply()
                            Toast.makeText(this, "bienvenido $usuario", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else{
                            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                        }
                    } else{
                        Toast.makeText(this, "El usuario no existe", Toast.LENGTH_SHORT).show()
                    }
                } .addOnFailureListener {
                    Toast.makeText(this, "error al conectar con firestore", Toast.LENGTH_SHORT).show()
                }
        }

    }

}
