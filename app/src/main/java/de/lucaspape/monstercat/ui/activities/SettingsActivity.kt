package de.lucaspape.monstercat.ui.activities

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
import de.lucaspape.monstercat.database.helper.ManualPlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.music.crossfade
import de.lucaspape.monstercat.music.playRelatedSongsAfterPlaylistFinished
import de.lucaspape.monstercat.music.volume
import de.lucaspape.monstercat.util.Auth
import de.lucaspape.util.Settings
import de.lucaspape.monstercat.util.displayInfo
import java.io.File
import java.io.FileNotFoundException
import kotlin.math.log

/**
 * SettingsActivity
 */
class SettingsActivity : AppCompatActivity() {
    private fun setupSwitches(){
        val streamMobileSwitch = findViewById<Switch>(R.id.streamMobileSwitch)
        val downloadMobileSwitch = findViewById<Switch>(R.id.downloadMobileSwitch)
        val downloadCoversMobileSwitch = findViewById<Switch>(R.id.downloadCoversMobileSwitch)
        val darkThemeSwitch = findViewById<Switch>(R.id.darkThemeSwitch)
        val disableAudioFocusSwitch = findViewById<Switch>(R.id.audioFocusSwitch)
        val downloadFlacSwitch = findViewById<Switch>(R.id.downloadFlacSwitch)
        val skipMonstercatSongsSwitch = findViewById<Switch>(R.id.skipMonstercatSongsSwitch)
        val useCustomApiSwitch = findViewById<Switch>(R.id.useCustomApi)
        val playRelatedSwitch = findViewById<Switch>(R.id.playRelatedSwitch)
        val saveCoverImagesToCacheSwitch = findViewById<Switch>(R.id.saveCoverImagesToCacheSwitch)

        val settings = Settings.getSettings(this)

        settings.getBoolean(getString(R.string.saveCoverImagesToCacheSetting))?.let {
            saveCoverImagesToCacheSwitch.isChecked = it
        }

        settings.getBoolean(getString(R.string.streamOverMobileSetting))?.let {
            streamMobileSwitch.isChecked = it
        }


        settings.getBoolean(getString(R.string.downloadOverMobileSetting))?.let {
            downloadMobileSwitch.isChecked = it
        }

        settings.getBoolean(getString(R.string.downloadCoversOverMobileSetting))?.let {
            downloadCoversMobileSwitch.isChecked = it
        }

        settings.getBoolean(getString(R.string.disableAudioFocusSetting))?.let {
            disableAudioFocusSwitch.isChecked = it
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            darkThemeSwitch.visibility = View.VISIBLE
        } else {
            darkThemeSwitch.visibility = View.GONE
        }

        settings.getBoolean(getString(R.string.darkThemeSetting))?.let {
            darkThemeSwitch.isChecked = it
        }

        settings.getString(getString(R.string.downloadTypeSetting))?.let {
            downloadFlacSwitch.isChecked = it == "flac"
        }

        settings.getBoolean(getString(R.string.skipMonstercatSongsSetting))?.let {
            skipMonstercatSongsSwitch.isChecked = it
        }

        settings.getBoolean(getString(R.string.useCustomApiSetting))?.let {
            useCustomApiSwitch.isChecked = it
        }

        settings.getBoolean(getString(R.string.playRelatedSetting))?.let {
            playRelatedSwitch.isChecked = it
        }

        //set switch listeners
        streamMobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(getString(R.string.streamOverMobileSetting), isChecked)
        }

        downloadMobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(getString(R.string.downloadOverMobileSetting), isChecked)
        }

        downloadCoversMobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(getString(R.string.downloadCoversOverMobileSetting), isChecked)
        }

        disableAudioFocusSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(getString(R.string.disableAudioFocusSetting), isChecked)
        }

        darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(getString(R.string.darkThemeSetting), isChecked)

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

        }

        downloadFlacSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                settings.setString(getString(R.string.downloadTypeSetting), "flac")
            } else {
                settings.setString(getString(R.string.downloadTypeSetting), "mp3_320")
            }
        }

        skipMonstercatSongsSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(getString(R.string.skipMonstercatSongsSetting), isChecked)
        }

        useCustomApiSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(getString(R.string.useCustomApiSetting), isChecked)
        }

        playRelatedSwitch.setOnCheckedChangeListener {_, isChecked ->
            settings.setBoolean(getString(R.string.playRelatedSetting), isChecked)
            playRelatedSongsAfterPlaylistFinished = isChecked
        }

        saveCoverImagesToCacheSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(getString(R.string.saveCoverImagesToCacheSetting), isChecked)
        }
    }

    private fun setupButtons(){
        val resetDatabaseButton = findViewById<Button>(R.id.resetDatabaseButton)

        resetDatabaseButton.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(this)
                .setTitle(getString(R.string.resetDatabase))
                .setMessage(getString(R.string.resetDatabaseQuestion))
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    AlbumDatabaseHelper(this).reCreateTable(this, true)
                    CatalogSongDatabaseHelper(this).reCreateTable()
                    PlaylistDatabaseHelper(this).reCreateTable(this, true)
                    ManualPlaylistDatabaseHelper(this).reCreateTable(this, true)

                    try {
                        File("$cacheDir/player_state.obj").delete()
                    } catch (e: FileNotFoundException) {

                    }

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

        val settings = Settings.getSettings(this)

        //login button listener
        findViewById<Button>(R.id.add_account).setOnClickListener {
            val usernameInput = findViewById<EditText>(R.id.usernameInput)
            val passwordInput = findViewById<EditText>(R.id.passwordInput)

            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            settings.setString(getString(R.string.emailSetting), username)
            settings.setString(getString(R.string.passwordSetting), password)

            Auth().login(this, username, password, {
                displayInfo(this, getString(R.string.loginSuccessfulMsg))
            }, {
                displayInfo(this, getString(R.string.loginFailedMsg))
            })
        }
    }

    private fun setupCoverResolutionSeekBar(){
        val coverResolutionSeekBar = findViewById<SeekBar>(R.id.coverResolutionSeekbar)
        val shownCoverResolution = findViewById<TextView>(R.id.shownCoverResolution)

        coverResolutionSeekBar.max = 2048 / 256

        val settings = Settings.getSettings(this)

        settings.getInt(getString(R.string.primaryCoverResolutionSetting))?.let {
            coverResolutionSeekBar.progress = it / 256
            shownCoverResolution.text = it.toString()
        }

        coverResolutionSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (progress != 0) {
                        settings.setInt(
                            getString(R.string.primaryCoverResolutionSetting),
                            (progress * 256)
                        )
                        settings.setInt(
                            getString(R.string.secondaryCoverResolutionSetting),
                            (((progress) * 256) / 4)
                        )
                    } else {
                        settings.setInt(getString(R.string.primaryCoverResolutionSetting), (128))
                        settings.setInt(getString(R.string.secondaryCoverResolutionSetting), (64))
                    }

                    settings.getInt(getString(R.string.primaryCoverResolutionSetting))
                        ?.let { shownCoverResolution.text = it.toString() }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }

    private fun setupCrossfadeSeekBar(){
        val crossfadeTimeSeekBar = findViewById<SeekBar>(R.id.crossfadeTimeSeekbar)
        val shownCrossfadeTime = findViewById<TextView>(R.id.shownCrossfadeTime)

        crossfadeTimeSeekBar.max = 20000 / 1000

        val settings = Settings.getSettings(this)

        settings.getInt(getString(R.string.crossfadeTimeSetting))?.let {
            crossfadeTimeSeekBar.progress = it / 1000
            shownCrossfadeTime.text = (it / 1000).toString()
            crossfade = it
        }


        crossfadeTimeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    settings.setInt(getString(R.string.crossfadeTimeSetting), (progress * 1000))

                    settings.getInt(getString(R.string.crossfadeTimeSetting))
                        ?.let {
                            shownCrossfadeTime.text = (it / 1000).toString()
                            crossfade = it
                        }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }

    private fun setupVolumeSeekBar(){
        val volumeSeekBar = findViewById<SeekBar>(R.id.volumeSeekBar)
        val shownVolume = findViewById<TextView>(R.id.shownVolumeText)

        volumeSeekBar.max = 100

        val settings = Settings.getSettings(this)

        settings.getFloat(getString(R.string.volumeSetting))?.let {
            volumeSeekBar.progress = (it*100).toInt()
            shownVolume.text = (it*100).toString()
            volume = 1-log(100-(it*100), 100.toFloat())
        }

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    settings.setFloat(getString(R.string.volumeSetting), (progress.toFloat() / 100.toFloat()))

                    settings.getFloat(getString(R.string.volumeSetting))
                        ?.let {
                            shownVolume.text = ((it*100).toInt()).toString()
                            volume = 1-log(100-(it*100), 100.toFloat())
                        }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        setupSwitches()
        setupButtons()
        setupCoverResolutionSeekBar()
        setupCrossfadeSeekBar()
        setupVolumeSeekBar()
    }
}