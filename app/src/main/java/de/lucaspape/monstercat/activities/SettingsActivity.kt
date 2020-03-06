package de.lucaspape.monstercat.activities

import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.music.crossfade
import de.lucaspape.monstercat.util.Auth
import de.lucaspape.monstercat.util.Settings
import de.lucaspape.monstercat.util.displayInfo

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
            settings.saveSetting(getString(R.string.emailSetting), username)
            settings.saveSetting(getString(R.string.passwordSetting), password)

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

        val crossfadeTimeSeekBar = findViewById<SeekBar>(R.id.crossfadeTimeSeekbar)
        val shownCrossfadeTime = findViewById<TextView>(R.id.shownCrossfadeTime)

        crossfadeTimeSeekBar.max = 20000 / 1000

        val skipMonstercatSongsSwitch = findViewById<Switch>(R.id.skipMonstercatSongsSwitch)
        val resetDatabaseButton = findViewById<Button>(R.id.resetDatabaseButton)

        if (settings.getSetting(getString(R.string.streamOverMobileSetting)) != null) {
            streamMobileSwitch.isChecked = settings.getSetting(getString(R.string.streamOverMobileSetting))!!.toBoolean()
        }

        if (settings.getSetting(getString(R.string.downloadOverMobileSetting)) != null) {
            downloadMobileSwitch.isChecked = settings.getSetting(getString(R.string.downloadOverMobileSetting))!!.toBoolean()
        }

        if (settings.getSetting(getString(R.string.downloadCoversOverMobileSetting)) != null) {
            downloadCoversMobileSwitch.isChecked =
                settings.getSetting(getString(R.string.downloadCoversOverMobileSetting))!!.toBoolean()
        }

        if (settings.getSetting(getString(R.string.disableAudioFocusSetting)) != null) {
            disableAudioFocusSwitch.isChecked =
                settings.getSetting(getString(R.string.disableAudioFocusSetting))!!.toBoolean()
        }

        if (settings.getSetting(getString(R.string.downloadStreamSetting)) != null) {
            downloadStreamSwitch.isChecked =
                settings.getSetting(getString(R.string.downloadStreamSetting))!!.toBoolean()
        }

        if (settings.getSetting(getString(R.string.primaryCoverResolutionSetting)) != null) {
            coverResolutionSeekBar.progress =
                Integer.parseInt(settings.getSetting(getString(R.string.primaryCoverResolutionSetting))!!) / 256
            shownCoverResolution.text = settings.getSetting(getString(R.string.primaryCoverResolutionSetting))!!
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            darkThemeSwitch.visibility = View.VISIBLE
        } else {
            darkThemeSwitch.visibility = View.GONE
        }

        if (settings.getSetting(getString(R.string.darkThemeSetting)) != null) {
            darkThemeSwitch.isChecked = settings.getSetting(getString(R.string.darkThemeSetting))!!.toBoolean()
        }

        if (settings.getSetting(getString(R.string.downloadTypeSetting)) != null) {
            downloadFlacSwitch.isChecked = settings.getSetting(getString(R.string.downloadTypeSetting)) == "flac"
        }

        if (settings.getSetting(getString(R.string.skipMonstercatSongsSetting)) != null) {
            skipMonstercatSongsSwitch.isChecked =
                settings.getSetting(getString(R.string.skipMonstercatSongsSetting))!!.toBoolean()
        }

        if (settings.getSetting(getString(R.string.crossfadeTimeSetting)) != null) {
            crossfadeTimeSeekBar.progress =
                Integer.parseInt(settings.getSetting(getString(R.string.crossfadeTimeSetting))!!) / 1000
            shownCrossfadeTime.text =
                (Integer.parseInt(settings.getSetting(getString(R.string.crossfadeTimeSetting))!!) / 1000).toString()
            settings.getSetting(getString(R.string.crossfadeTimeSetting))?.let {
                crossfade = Integer.parseInt(it)
            }
        }

        //set switch listeners
        streamMobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting(getString(R.string.streamOverMobileSetting), isChecked.toString())
        }

        downloadMobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting(getString(R.string.downloadOverMobileSetting), isChecked.toString())
        }

        downloadCoversMobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting(getString(R.string.downloadCoversOverMobileSetting), isChecked.toString())
        }

        disableAudioFocusSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting(getString(R.string.disableAudioFocusSetting), isChecked.toString())
        }

        downloadStreamSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting(getString(R.string.downloadStreamSetting), isChecked.toString())
        }


        darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting(getString(R.string.darkThemeSetting), isChecked.toString())

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

        }

        coverResolutionSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (progress != 0) {
                        settings.saveSetting(getString(R.string.primaryCoverResolutionSetting), (progress * 256).toString())
                        settings.saveSetting(
                            getString(R.string.secondaryCoverResolutionSetting),
                            (((progress) * 256) / 4).toString()
                        )
                    } else {
                        settings.saveSetting(getString(R.string.primaryCoverResolutionSetting), (128).toString())
                        settings.saveSetting(getString(R.string.secondaryCoverResolutionSetting), (64).toString())
                    }

                    settings.getSetting(getString(R.string.primaryCoverResolutionSetting))
                        ?.let { shownCoverResolution.text = it }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        crossfadeTimeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    settings.saveSetting(getString(R.string.crossfadeTimeSetting), (progress * 1000).toString())

                    settings.getSetting(getString(R.string.crossfadeTimeSetting))
                        ?.let { shownCrossfadeTime.text = (Integer.parseInt(it) / 1000).toString() }

                    settings.getSetting(getString(R.string.crossfadeTimeSetting))?.let {
                        crossfade = Integer.parseInt(it)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        downloadFlacSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                settings.saveSetting(getString(R.string.downloadTypeSetting), "flac")
            } else {
                settings.saveSetting(getString(R.string.downloadTypeSetting), "mp3_320")
            }
        }

        skipMonstercatSongsSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.saveSetting(getString(R.string.skipMonstercatSongsSetting), isChecked.toString())
        }

        resetDatabaseButton.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(this)
                .setTitle(getString(R.string.resetDatabase))
                .setMessage(getString(R.string.resetDatabaseQuestion))
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    AlbumDatabaseHelper(this).reCreateTable(this, true)
                    CatalogSongDatabaseHelper(this).reCreateTable()
                    PlaylistDatabaseHelper(this).reCreateTable(this, true)

                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)

                    finish()

                    Runtime.getRuntime().exit(0)
                }
                .setNegativeButton(android.R.string.no, null)

            val dialog = alertDialogBuilder.create()
            dialog.show()

            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setTextColor(typedValue.data)

            val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            negativeButton.setTextColor(typedValue.data)
        }
    }
}