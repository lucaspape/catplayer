package de.lucaspape.monstercat.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.Auth
import de.lucaspape.monstercat.settings.Settings

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.add_account).setOnClickListener {
            val usernameInput = findViewById<EditText>(R.id.usernameInput)
            val passwordInput = findViewById<EditText>(R.id.passwordInput)

            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            val settings = Settings(this)
            settings.saveSetting("email", username)
            settings.saveSetting("password", password)

            Auth().login(this)
        }
    }
}