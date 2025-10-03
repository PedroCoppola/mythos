package com.mythos.mythos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edtUsuario = findViewById<EditText>(R.id.edtUsuarioRegistro)
        val edtPassword = findViewById<EditText>(R.id.edtPasswordRegistro)
        val btnRegistrar = findViewById<Button>(R.id.btnRegister)
        val btnBack = findViewById<Button>(R.id.btnBack)

        db = FirebaseFirestore.getInstance()

        btnBack.setOnClickListener {
            finish()
        }

        btnRegistrar.setOnClickListener {
            val usuario = edtUsuario.text.toString().trim()
            val contraseña = edtPassword.text.toString().trim()

            if (usuario.isEmpty() || contraseña.isEmpty()) {
                Toast.makeText(this, "Por favor completa los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.collection("usuarios").document(usuario).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        Toast.makeText(this, "El usuario ya existe", Toast.LENGTH_SHORT).show()
                    } else {
                        val nuevoUsuario = hashMapOf(
                            "usuario" to usuario,
                            "contraseña" to contraseña
                        )
                        db.collection("usuarios").document(usuario).set(nuevoUsuario)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Error al registrar", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al conectar con firestore", Toast.LENGTH_SHORT).show()
                }
        }

    }
}