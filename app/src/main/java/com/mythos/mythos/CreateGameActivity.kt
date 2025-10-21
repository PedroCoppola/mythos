package com.mythos.mythos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider

class CreateGameActivity : AppCompatActivity() {

    // --- Vistas ---
    private lateinit var etContext: EditText
    private lateinit var etCharacter: EditText
    private lateinit var sliderTone: Slider // CORRECCIÓN: ID correcto
    private lateinit var sliderDialogue: Slider // CORRECCIÓN: ID correcto
    private lateinit var btnCreateGame: Button // CORRECCIÓN: ID correcto
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_game)

        // --- Vinculación de Vistas ---
        etContext = findViewById(R.id.etContext)
        etCharacter = findViewById(R.id.etCharacter)
        sliderTone = findViewById(R.id.sliderTone) // CORRECCIÓN: ID correcto
        sliderDialogue = findViewById(R.id.sliderDialogue) // CORRECCIÓN: ID correcto
        btnCreateGame = findViewById(R.id.btnCreateGame) // CORRECCIóN: ID correcto
        btnBack = findViewById(R.id.btnBack)

        // --- Listeners ---
        btnBack.setOnClickListener {
            finish() // Cierra la actividad y vuelve a la anterior (HomePage)
        }

        btnCreateGame.setOnClickListener {
            createAndStartGame()
        }
    }

    private fun createAndStartGame() {
        val context = etContext.text.toString().trim()
        val character = etCharacter.text.toString().trim()
        val toneValue = sliderTone.value.toInt()
        val dialogueValue = sliderDialogue.value.toInt()

        // Creamos el Intent para ir a MainActivity
        val intent = Intent(this, MainActivity::class.java)

        // Pasamos los parámetros de la nueva partida
        // Si el usuario no escribe nada, le pasamos null para que MainActivity use los valores por defecto
        intent.putExtra("GAME_CONTEXT", if (context.isNotEmpty()) context else null)
        intent.putExtra("GAME_CHARACTER", if (character.isNotEmpty()) character else null)
        // Pasamos los valores de los sliders (aunque no los usamos todavía en MainActivity, es una buena práctica)
        intent.putExtra("GAME_TONE", toneValue)
        intent.putExtra("GAME_DIALOGUE", dialogueValue)

        startActivity(intent)
    }
}
