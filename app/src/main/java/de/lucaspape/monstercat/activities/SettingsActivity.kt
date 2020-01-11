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
import de.lucaspape.monstercat.util.displayInfo
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

            Auth().login(this, username, password, {
                displayInfo(this, getString(R.string.loginSuccessfulMsg))
            }, {
                displayInfo(this, getString(R.string.loginFailedMsg))
            })
        }

        //set the switches to the saved value
        val streamMobileSwitch = findViewById<Switch>(R.id.streamMobileSwitch)
        val downloadMobileSwitch = findViewById<Switch>(R.id.downloadMobileSwitch)
        val downloadCoversMobileSwitch = findViewById<Switch>(R.id.downloadCoversMobileSwitch)
        val darkThemeSwitch = findViewById<Switch>(R.id.darkThemeSwitch)
        val disableAudioFocusSwitch = findViewById<Switch>(R.id.audioFocusSwitch)
        val downloadStreamSwitch = findViewById<Switch>(R.id.streamDownloadSwitch)

        val downloadFlacSwitch = findViewById<Switch>(R.id.downloadFlacSwitch)

        val coverResolutionSeekBar = findViewById<SeekBar>(R.id.coverResolutionSeekbar)
        val shownCoverResolution = findViewById<TextView>(R.id.shownCoverResolution)

        coverResolutionSeekBar.max = 2048 / 256

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

        if (settings.getSetting("downloadStream") != null) {
            downloadStreamSwitch.isChecked =
                settings.getSetting("downloadStream")!!.toBoolean()
        }

        if (settings.getSetting("primaryCoverResolution") != null) {
            coverResolutionSeekBar.progress = Integer.parseInt(settings.getSetting("primaryCoverResolution")!!) / 256
            shownCoverResolution.text = settings.getSetting("primaryCoverResolution")!!
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            darkThemeSwitch.visibility = View.VISIBLE
        } else {
            darkThemeSwitch.visibility = View.GONE
        }

        if (settings.getSetting("darkTheme") != null) {
            darkThemeSwitch.isChecked = settings.getSetting("darkTheme")!!.toBoolean()
        }

        if (settings.getSetting("downloadType") != null) {
            downloadFlacSwitch.isChecked = settings.getSetting("downloadType") == "flac"
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

        downloadStreamSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting("downloadStream", isChecked.toString())
        }


        darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting("darkTheme", isChecked.toString())

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

        }

        coverResolutionSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser){
                    if(progress != 0){
                        settings.saveSetting("primaryCoverResolution", (progress*256).toString())
                        settings.saveSetting("secondaryCoverResolution", (((progress)*256)/10).toString())
                    }else{
                        settings.saveSetting("primaryCoverResolution", (128).toString())
                        settings.saveSetting("secondaryCoverResolution", (64).toString())
                    }

                    settings.getSetting("primaryCoverResolution")?.let { shownCoverResolution.text = it }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        downloadFlacSwitch.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                settings.saveSetting("downloadType", "flac")
            }else{
                settings.saveSetting("downloadType", "mp3_320")
            }
        }
    }
}