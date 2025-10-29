package com.mythos.mythos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Espera 2 segundos y decide adónde ir
        window.decorView.postDelayed({
            val auth = FirebaseAuth.getInstance()
            val nextActivity = Intent(this, HomePageActivity::class.java)
            startActivity(nextActivity)
            finish()
        }, 2000) // 2 segundos de splash
    }
}
