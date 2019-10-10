package de.lucaspape.monstercat.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.Auth
import de.lucaspape.monstercat.settings.Settings

/**
 * SettingsActivity
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        val settings = Settings(this)

        //login button listener
        findViewById<Button>(R.id.add_account).setOnClickListener {
            val usernameInput = findViewById<EditText>(R.id.usernameInput)
            val passwordInput = findViewById<EditText>(R.id.passwordInput)

            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            settings.saveSetting("email", username)
            settings.saveSetting("password", password)

            Auth().login(this)
        }

        //set the switches to the saved value
        val streamMobileSwitch = findViewById<Switch>(R.id.streamMobileSwitch)
        val downloadMobileSwitch = findViewById<Switch>(R.id.downloadMobileSwitch)
        val downloadCoversMobileSwitch = findViewById<Switch>(R.id.downloadCoversMobileSwitch)

        if(settings.getSetting("streamOverMobile") != null){
            streamMobileSwitch.isChecked = settings.getSetting("streamOverMobile")!!.toBoolean()
        }

        if(settings.getSetting("downloadOverMobile") != null){
            downloadMobileSwitch.isChecked = settings.getSetting("downloadOverMobile")!!.toBoolean()
        }

        if(settings.getSetting("downloadCoversOverMobile") != null){
            downloadCoversMobileSwitch.isChecked = settings.getSetting("downloadCoversOverMobile")!!.toBoolean()
        }


        //set switch listeners
        streamMobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting("streamOverMobile", isChecked.toString())
        }

        downloadMobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting("downloadOverMobile", isChecked.toString())
        }

        downloadCoversMobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting("downloadCoversOverMobile", isChecked.toString())
        }



    }
}