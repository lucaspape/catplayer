package de.lucaspape.monstercat.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.Auth
import de.lucaspape.monstercat.util.Settings
import kotlinx.android.synthetic.main.activity_settings.*

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
        val darkThemeSwitch = findViewById<Switch>(R.id.darkThemeSwitch)
        val disableAudioFocusSwitch = findViewById<Switch>(R.id.audioFocusSwitch)
        val maxLoadSeekBar = findViewById<SeekBar>(R.id.maximumLoadSeekBar)
        val shownMaxValue = findViewById<TextView>(R.id.shownMaxValue)

        maxLoadSeekBar.max = 200 / 50

        if (settings.getSetting("streamOverMobile") != null) {
            streamMobileSwitch.isChecked = settings.getSetting("streamOverMobile")!!.toBoolean()
        }

        if (settings.getSetting("downloadOverMobile") != null) {
            downloadMobileSwitch.isChecked = settings.getSetting("downloadOverMobile")!!.toBoolean()
        }

        if (settings.getSetting("downloadCoversOverMobile") != null) {
            downloadCoversMobileSwitch.isChecked =
                settings.getSetting("downloadCoversOverMobile")!!.toBoolean()
        }

        if (settings.getSetting("disableAudioFocus") != null) {
            disableAudioFocusSwitch.isChecked =
                settings.getSetting("disableAudioFocus")!!.toBoolean()
        }

        if (settings.getSetting("maximumLoad") != null) {
            maxLoadSeekBar.progress = Integer.parseInt(settings.getSetting("maximumLoad")!!) / 50
            shownMaxValue.text = settings.getSetting("maximumLoad")!!
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            darkThemeSwitch.visibility = View.VISIBLE
        } else {
            darkThemeSwitch.visibility = View.GONE
        }

        if (settings.getSetting("darkTheme") != null) {
            darkThemeSwitch.isChecked = settings.getSetting("darkTheme")!!.toBoolean()
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

        disableAudioFocusSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting("disableAudioFocus", isChecked.toString())
        }

        darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting("darkTheme", isChecked.toString())

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

        }

        maximumLoadSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    settings.saveSetting("maximumLoad", (progress * 50).toString())
                    shownMaxValue.text = settings.getSetting("maximumLoad")!!
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
    }
}