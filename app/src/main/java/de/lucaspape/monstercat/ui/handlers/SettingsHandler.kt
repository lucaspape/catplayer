package de.lucaspape.monstercat.ui.handlers

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.database.helper.ManualPlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.music.crossfade
import de.lucaspape.monstercat.music.playRelatedSongsAfterPlaylistFinished
import de.lucaspape.monstercat.music.volume
import de.lucaspape.monstercat.request.async.checkCustomApiFeaturesAsync
import de.lucaspape.monstercat.ui.activities.MainActivity
import de.lucaspape.monstercat.util.Auth
import de.lucaspape.util.Settings
import de.lucaspape.monstercat.util.displayInfo
import de.lucaspape.monstercat.util.displaySnackBar
import java.io.File
import java.io.FileNotFoundException
import kotlin.math.log

/**
 * SettingsActivity
 */
class SettingsHandler(private val closeSettings:() -> Unit) : Handler {
    private fun setupSwitches(view: View){
        val streamMobileSwitch = view.findViewById<Switch>(R.id.streamMobileSwitch)
        val downloadMobileSwitch = view.findViewById<Switch>(R.id.downloadMobileSwitch)
        val downloadCoversMobileSwitch = view.findViewById<Switch>(R.id.downloadCoversMobileSwitch)
        val liveInfoSwitch = view.findViewById<Switch>(R.id.liveInfoSwitch)
        val darkThemeSwitch = view.findViewById<Switch>(R.id.darkThemeSwitch)
        val disableAudioFocusSwitch = view.findViewById<Switch>(R.id.audioFocusSwitch)
        val downloadFlacSwitch = view.findViewById<Switch>(R.id.downloadFlacSwitch)
        val skipMonstercatSongsSwitch = view.findViewById<Switch>(R.id.skipMonstercatSongsSwitch)
        val useCustomApiSwitch = view.findViewById<Switch>(R.id.useCustomApi)
        val playRelatedSwitch = view.findViewById<Switch>(R.id.playRelatedSwitch)
        val saveCoverImagesToCacheSwitch = view.findViewById<Switch>(R.id.saveCoverImagesToCacheSwitch)

        val settings = Settings.getSettings(view.context)

        settings.getBoolean(view.context.getString(R.string.saveCoverImagesToCacheSetting))?.let {
            saveCoverImagesToCacheSwitch.isChecked = it
        }

        settings.getBoolean(view.context.getString(R.string.streamOverMobileSetting))?.let {
            streamMobileSwitch.isChecked = it
        }


        settings.getBoolean(view.context.getString(R.string.downloadOverMobileSetting))?.let {
            downloadMobileSwitch.isChecked = it
        }

        settings.getBoolean(view.context.getString(R.string.downloadCoversOverMobileSetting))?.let {
            downloadCoversMobileSwitch.isChecked = it
        }

        settings.getBoolean(view.context.getString(R.string.disableAudioFocusSetting))?.let {
            disableAudioFocusSwitch.isChecked = it
        }

        settings.getBoolean(view.context.getString(R.string.liveInfoSetting))?.let {
            liveInfoSwitch.isChecked = it
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            darkThemeSwitch.visibility = View.VISIBLE
        } else {
            darkThemeSwitch.visibility = View.GONE
        }

        settings.getBoolean(view.context.getString(R.string.darkThemeSetting))?.let {
            darkThemeSwitch.isChecked = it
        }

        settings.getString(view.context.getString(R.string.downloadTypeSetting))?.let {
            downloadFlacSwitch.isChecked = it == "flac"
        }

        settings.getBoolean(view.context.getString(R.string.skipMonstercatSongsSetting))?.let {
            skipMonstercatSongsSwitch.isChecked = it
        }

        settings.getBoolean(view.context.getString(R.string.useCustomApiSetting))?.let {
            useCustomApiSwitch.isChecked = it
        }

        settings.getBoolean(view.context.getString(R.string.playRelatedSetting))?.let {
            playRelatedSwitch.isChecked = it
        }

        //set switch listeners
        streamMobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(view.context.getString(R.string.streamOverMobileSetting), isChecked)
        }

        downloadMobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(view.context.getString(R.string.downloadOverMobileSetting), isChecked)
        }

        downloadCoversMobileSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(view.context.getString(R.string.downloadCoversOverMobileSetting), isChecked)
        }

        disableAudioFocusSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(view.context.getString(R.string.disableAudioFocusSetting), isChecked)
        }

        darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(view.context.getString(R.string.darkThemeSetting), isChecked)

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

        }

        liveInfoSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(view.context.getString(R.string.liveInfoSetting), isChecked)
        }

        downloadFlacSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                settings.setString(view.context.getString(R.string.downloadTypeSetting), "flac")
            } else {
                settings.setString(view.context.getString(R.string.downloadTypeSetting), "mp3_320")
            }
        }

        skipMonstercatSongsSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(view.context.getString(R.string.skipMonstercatSongsSetting), isChecked)
        }

        useCustomApiSwitch.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                //check for custom api features
                checkCustomApiFeaturesAsync(view.context, {
                    useCustomApiSwitch.isChecked = true
                    settings.setBoolean(view.context.getString(R.string.useCustomApiSetting), true)
                    displaySnackBar(view, view.context.getString(R.string.customApiEnabledMsg), null) {}
                }, {
                    useCustomApiSwitch.isChecked = false
                    settings.setBoolean(view.context.getString(R.string.useCustomApiSetting), false)
                    displaySnackBar(view, view.context.getString(R.string.customApiEnableError), null) {}
                })
            }else{
                settings.setBoolean(view.context.getString(R.string.useCustomApiSetting), false)
            }

        }

        playRelatedSwitch.setOnCheckedChangeListener {_, isChecked ->
            settings.setBoolean(view.context.getString(R.string.playRelatedSetting), isChecked)
            playRelatedSongsAfterPlaylistFinished = isChecked
        }

        saveCoverImagesToCacheSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.setBoolean(view.context.getString(R.string.saveCoverImagesToCacheSetting), isChecked)
        }
    }

    private fun setupButtons(view: View){
        val resetDatabaseButton = view.findViewById<Button>(R.id.resetDatabaseButton)

        resetDatabaseButton.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(view.context)
                .setTitle(view.context.getString(R.string.resetDatabase))
                .setMessage(view.context.getString(R.string.resetDatabaseQuestion))
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    AlbumDatabaseHelper(view.context).reCreateTable(view.context, true)
                    CatalogSongDatabaseHelper(view.context).reCreateTable()
                    PlaylistDatabaseHelper(view.context).reCreateTable(view.context, true)
                    ManualPlaylistDatabaseHelper(view.context).reCreateTable(view.context, true)

                    try {
                        File("${view.context.cacheDir}/player_state.obj").delete()
                    } catch (e: FileNotFoundException) {

                    }

                    val intent = Intent(view.context, MainActivity::class.java)
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    view.context.startActivity(intent)

                    Runtime.getRuntime().exit(0)
                }
                .setNegativeButton(android.R.string.no, null)

            val dialog = alertDialogBuilder.create()
            dialog.show()

            val typedValue = TypedValue()
            view.context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setTextColor(typedValue.data)

            val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            negativeButton.setTextColor(typedValue.data)
        }

        val settings = Settings.getSettings(view.context)

        //login button listener
        view.findViewById<Button>(R.id.add_account).setOnClickListener {
            val usernameInput = view.findViewById<EditText>(R.id.usernameInput)
            val passwordInput = view.findViewById<EditText>(R.id.passwordInput)

            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            settings.setString(view.context.getString(R.string.emailSetting), username)
            settings.setString(view.context.getString(R.string.passwordSetting), password)

            Auth().login(view.context, username, password, {
                displayInfo(view.context, view.context.getString(R.string.loginSuccessfulMsg))
            }, {
                displayInfo(view.context, view.context.getString(R.string.loginFailedMsg))
            })
        }

        view.findViewById<Button>(R.id.customApiInputButton).setOnClickListener {
            MaterialAlertDialogBuilder(view.context).apply {
                val layoutInflater =
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                val customApiInputLayout =
                    layoutInflater.inflate(R.layout.customapi_input_layout, null)

                val customApiEditText =
                    customApiInputLayout.findViewById<EditText>(R.id.customApiInput)

                settings.getString(context.getString(R.string.customApiBaseUrlSetting))?.let {
                    customApiEditText.setText(it)
                }

                setTitle(view.context.getString(R.string.setCustomApiUrl))

                setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                    val newCustomApiUrl = customApiEditText.text.toString()
                    settings.setString(view.context.getString(R.string.customApiBaseUrlSetting), newCustomApiUrl)
                    settings.setBoolean(view.context.getString(R.string.useCustomApiSetting), false)
                    view.findViewById<Switch>(R.id.useCustomApi).isChecked = false
                }

                setView(customApiInputLayout)
                setCancelable(true)
            }.create().run {
                show()

                val typedValue = TypedValue()
                context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

                val positiveButton = getButton(DialogInterface.BUTTON_POSITIVE)
                positiveButton.setTextColor(typedValue.data)

                val negativeButton = getButton(DialogInterface.BUTTON_NEGATIVE)
                negativeButton.setTextColor(typedValue.data)
            }
        }
    }

    private fun setupCoverResolutionSeekBar(view: View){
        val coverResolutionSeekBar = view.findViewById<SeekBar>(R.id.coverResolutionSeekbar)
        val shownCoverResolution = view.findViewById<TextView>(R.id.shownCoverResolution)

        coverResolutionSeekBar.max = 2048 / 256

        val settings = Settings.getSettings(view.context)

        settings.getInt(view.context.getString(R.string.primaryCoverResolutionSetting))?.let {
            coverResolutionSeekBar.progress = it / 256
            shownCoverResolution.text = it.toString()
        }

        coverResolutionSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (progress != 0) {
                        settings.setInt(
                            view.context.getString(R.string.primaryCoverResolutionSetting),
                            (progress * 256)
                        )
                        settings.setInt(
                            view.context.getString(R.string.secondaryCoverResolutionSetting),
                            (((progress) * 256) / 4)
                        )
                    } else {
                        settings.setInt(view.context.getString(R.string.primaryCoverResolutionSetting), (128))
                        settings.setInt(view.context.getString(R.string.secondaryCoverResolutionSetting), (64))
                    }

                    settings.getInt(view.context.getString(R.string.primaryCoverResolutionSetting))
                        ?.let { shownCoverResolution.text = it.toString() }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }

    private fun setupCrossfadeSeekBar(view: View){
        val crossfadeTimeSeekBar = view.findViewById<SeekBar>(R.id.crossfadeTimeSeekbar)
        val shownCrossfadeTime = view.findViewById<TextView>(R.id.shownCrossfadeTime)

        crossfadeTimeSeekBar.max = 20000 / 1000

        val settings = Settings.getSettings(view.context)

        settings.getInt(view.context.getString(R.string.crossfadeTimeSetting))?.let {
            crossfadeTimeSeekBar.progress = it / 1000
            shownCrossfadeTime.text = (it / 1000).toString()
            crossfade = it
        }


        crossfadeTimeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    settings.setInt(view.context.getString(R.string.crossfadeTimeSetting), (progress * 1000))

                    settings.getInt(view.context.getString(R.string.crossfadeTimeSetting))
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

    private fun setupVolumeSeekBar(view: View){
        val volumeSeekBar = view.findViewById<SeekBar>(R.id.volumeSeekBar)
        val shownVolume = view.findViewById<TextView>(R.id.shownVolumeText)

        volumeSeekBar.max = 100

        val settings = Settings.getSettings(view.context)

        settings.getFloat(view.context.getString(R.string.volumeSetting))?.let {
            volumeSeekBar.progress = (it*100).toInt()
            shownVolume.text = (it*100).toString()
            volume = 1-log(100-(it*100), 100.toFloat())
        }

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    settings.setFloat(view.context.getString(R.string.volumeSetting), (progress.toFloat() / 100.toFloat()))

                    settings.getFloat(view.context.getString(R.string.volumeSetting))
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

    override val layout: Int = R.layout.fragment_settings

    override fun onBackPressed(view: View) {
        closeSettings()
    }

    override fun onPause(view: View) {

    }

    override fun onCreate(view: View) {
        setupSwitches(view)
        setupButtons(view)
        setupCoverResolutionSeekBar(view)
        setupCrossfadeSeekBar(view)
        setupVolumeSeekBar(view)
    }
}