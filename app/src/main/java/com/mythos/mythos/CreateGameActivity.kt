package com.mythos.mythos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CreateGameActivity : AppCompatActivity() {

    private lateinit var etContext: EditText
    private lateinit var etCharacter: EditText
    private lateinit var seekTone: SeekBar
    private lateinit var seekDialogue: SeekBar
    private lateinit var btnStartNewGame: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_game)

        etContext = findViewById(R.id.etContext)
        etCharacter = findViewById(R.id.etCharacter)
        seekTone = findViewById(R.id.seekTone)
        seekDialogue = findViewById(R.id.seekDialogue)
        btnStartNewGame = findViewById(R.id.btnStartNewGame)

        btnStartNewGame.setOnClickListener {
            val context = etContext.text.toString().trim()
            val character = etCharacter.text.toString().trim()
            val toneValue = seekTone.progress
            val dialogueValue = seekDialogue.progress

            // Creamos el Intent para volver a MainActivity
            val resultIntent = Intent()

            // Empaquetamos todos los datos que MainActivity necesitará
            resultIntent.putExtra("GAME_MODE", "CUSTOM") // Le decimos a Main que es una partida personalizada
            resultIntent.putExtra("GAME_CONTEXT", context)
            resultIntent.putExtra("GAME_CHARACTER", character)
            resultIntent.putExtra("GAME_TONE", toneValue)
            resultIntent.putExtra("GAME_DIALOGUE", dialogueValue)

            // Establecemos el resultado como OK y enviamos los datos de vuelta
            setResult(Activity.RESULT_OK, resultIntent)

            // Cerramos esta pantalla para volver a la anterior (que será ProfileActivity)
            // y el resultado se enviará a quien la haya llamado.
            finish()
        }
    }
}
