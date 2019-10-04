package de.lucaspape.monstercat.activities

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import de.lucaspape.monstercat.R

class SplashScreenActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val intent = Intent(
            applicationContext,
            MainActivity::class.java
        )
        startActivity(intent)
        finish()
    }
}