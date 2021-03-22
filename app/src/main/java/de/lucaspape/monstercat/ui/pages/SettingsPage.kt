package de.lucaspape.monstercat.ui.pages

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.ItemDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.ManualPlaylistDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.core.music.applyFilterSettings
import de.lucaspape.monstercat.core.music.crossfade
import de.lucaspape.monstercat.core.music.playRelatedSongsAfterPlaylistFinished
import de.lucaspape.monstercat.core.music.volume
import de.lucaspape.monstercat.ui.abstract_items.settings.*
import de.lucaspape.monstercat.ui.activities.MainActivity
import de.lucaspape.monstercat.ui.displayInfo
import de.lucaspape.monstercat.ui.displaySnackBar
import de.lucaspape.monstercat.util.*
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.request.async.checkCustomApiFeatures
import de.lucaspape.monstercat.ui.abstract_items.util.SpacerItem
import de.lucaspape.monstercat.ui.activities.genericScope
import de.lucaspape.monstercat.ui.pages.util.Page
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import kotlin.math.log

/**
 * SettingsActivity
 */
class SettingsPage(private val closeSettings: () -> Unit) : Page() {
    constructor() : this({})

    companion object {
        @JvmStatic
        val settingsPageName = "settings"
    }

    override fun onCreate(view: View) {
        setupRecyclerView(view)

        //spacer
        itemAdapter.add(
            SettingsLabelItem(
                ""
            )
        )

        addLogin(view)
        addPlaybackSettings(view)
        addDataSettings(view)
        addAdvancedSettings(view)
        addCustomApiButton(view)
        addResetDatabaseButton(view)

        loggedInStateChangedListeners.add(LoggedInStateChangedListener({
            onCreate(view)
        }, true))

        itemAdapter.add(SpacerItem())
    }

    override val pageName: String = settingsPageName

    override val layout: Int = R.layout.fragment_settings

    override fun onBackPressed(view: View):Boolean {
        closeSettings()
        return false
    }

    override fun onPause(view: View) {

    }

    private var recyclerView: RecyclerView? = null
    private var itemAdapter: ItemAdapter<GenericItem> = ItemAdapter()

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.settingsRecyclerView)

        recyclerView?.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        itemAdapter = ItemAdapter()

        val fastAdapter: FastAdapter<GenericItem> = FastAdapter.with(listOf(itemAdapter))

        recyclerView?.adapter = fastAdapter

        fastAdapter.addEventHook(object : ClickEventHook<GenericItem>() {
            override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                return when (viewHolder) {
                    is SettingsToggleItem.ViewHolder -> {
                        viewHolder.alertItemSwitch
                    }
                    is SettingsButtonItem.ViewHolder -> {
                        viewHolder.button
                    }
                    is SettingsLoginItem.ViewHolder -> {
                        viewHolder.button
                    }
                    is SettingsProfileItem.ViewHolder -> {
                        viewHolder.button
                    }
                    else -> null
                }
            }

            override fun onClick(
                v: View,
                position: Int,
                fastAdapter: FastAdapter<GenericItem>,
                item: GenericItem
            ) {
                if (item is SettingsToggleItem && v is SwitchMaterial) {
                    v.isChecked = item.onSwitchChange(item.setting, v.isChecked, v)
                } else if (item is SettingsButtonItem && v is Button) {
                    item.onClick()
                } else if (item is SettingsLoginItem) {
                    val usernameTextInput = view.findViewById<EditText>(R.id.settings_usernameInput)
                    val passwordTextInput = view.findViewById<EditText>(R.id.settings_passwordInput)

                    item.onLogin(
                        usernameTextInput.text.toString(),
                        passwordTextInput.text.toString()
                    )
                } else if (item is SettingsProfileItem) {
                    item.onLogout()
                }
            }
        })
    }

    private fun addDataSettings(view: View) {
        val settings = Settings.getSettings(view.context)

        itemAdapter.add(
            SettingsLabelItem(
                view.context.getString(R.string.dataSettings)
            )
        )

        val changeSetting: (setting: String, value: Boolean, switch: SwitchMaterial) -> Boolean =
            { setting, value, _ ->
                settings.setBoolean(setting, value)
                value
            }

        itemAdapter.add(
            SettingsToggleItem(
                view.context.getString(R.string.streamOverMobileSetting),
                true,
                view.context.getString(R.string.allowStreamMobile),
                null,
                changeSetting
            )
        )
        itemAdapter.add(
            SettingsToggleItem(
                view.context.getString(R.string.downloadOverMobileSetting),
                true,
                view.context.getString(R.string.allowDownloadMobile),
                null,
                changeSetting
            )
        )
        itemAdapter.add(
            SettingsToggleItem(
                view.context.getString(R.string.downloadCoversOverMobileSetting),
                true,
                view.context.getString(R.string.allowCoverDownloadMobile),
                null,
                changeSetting
            )
        )

        addCoverResolutionSeekBar(view)
    }

    private fun addPlaybackSettings(view: View) {
        val settings = Settings.getSettings(view.context)

        itemAdapter.add(
            SettingsLabelItem(
                view.context.getString(R.string.playbackSettings)
            )
        )

        val changeSetting: (setting: String, value: Boolean, switch: SwitchMaterial) -> Boolean =
            { setting, value, _ ->
                settings.setBoolean(setting, value)
                value
            }

        val changeFilterSetting: (setting: String, value: Boolean, switch: SwitchMaterial) -> Boolean =
            { setting, value, _ ->
                settings.setBoolean(setting, value)
                applyFilterSettings(view.context)
                value
            }

        itemAdapter.add(
            SettingsToggleItem(
                view.context.getString(R.string.blockNonCreatorFriendlySetting),
                true,
                view.context.getString(R.string.dontPlayNotCreatorFriendly),
                null,
                changeFilterSetting
            )
        )

        itemAdapter.add(
            SettingsToggleItem(
                view.context.getString(R.string.skipMonstercatSongsSetting),
                true,
                view.context.getString(R.string.skipSongsMonstercat),
                null,
                changeFilterSetting
            )
        )

        itemAdapter.add(
            SettingsToggleItem(
                view.context.getString(R.string.skipExplicitSongsSetting),
                true,
                view.context.getString(R.string.skipExplicitSongs),
                null,
                changeFilterSetting
            )
        )

        itemAdapter.add(
            SettingsToggleItem(
                view.context.getString(R.string.hideToBeSkippedSetting),
                true,
                view.context.getString(R.string.hideToBeSkipped),
                null,
                changeFilterSetting
            )
        )

        itemAdapter.add(
            SettingsToggleItem(
                view.context.getString(R.string.playRelatedSetting),
                true,
                view.context.getString(R.string.playRelatedAfter),
                view.context.getString(R.string.customApiSupportsPlayingRelatedSongsSetting)
            ) { setting, value, _ ->
                playRelatedSongsAfterPlaylistFinished = value
                settings.setBoolean(setting, value)
                value
            }
        )

        addVolumeSeekBar(view)
        addCrossFadeSeekBar(view)
    }

    private fun addResetDatabaseButton(view: View) {
        itemAdapter.add(
            SettingsButtonItem(
                view.context.getString(R.string.resetDatabase)
            ) {
                val alertDialogBuilder = AlertDialog.Builder(view.context)
                    .setTitle(view.context.getString(R.string.resetDatabase))
                    .setMessage(view.context.getString(R.string.resetDatabaseQuestion))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        AlbumDatabaseHelper(view.context).reCreateTable(view.context, true)
                        ItemDatabaseHelper(view.context, "catalog").reCreateTable()
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
                    .setNegativeButton(android.R.string.cancel, null)

                val dialog = alertDialogBuilder.create()
                dialog.show()

                val typedValue = TypedValue()
                view.context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

                val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                positiveButton.setTextColor(typedValue.data)

                val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                negativeButton.setTextColor(typedValue.data)
            })
    }

    private fun addCustomApiButton(view: View) {
        itemAdapter.add(
            SettingsButtonItem(
                view.context.getString(R.string.setCustomApiUrl)
            ) {
                val settings = Settings.getSettings(view.context)

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
                        settings.setString(
                            view.context.getString(R.string.customApiBaseUrlSetting),
                            newCustomApiUrl
                        )

                        settings.setBoolean(
                            view.context.getString(R.string.useCustomApiForCatalogAndAlbumViewSetting),
                            false
                        )

                        //recreate settings to reload
                        onCreate(view)
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
            })
    }

    private fun addLogin(view: View) {
        itemAdapter.add(
            SettingsLabelItem(
                view.context.getString(R.string.accountSettings)
            )
        )

        when {
            loggedIn -> {
                itemAdapter.add(SettingsProfileItem(username) {
                    Auth().logout(view.context)
                })
            }
            waitingForLogin || offline -> {
                itemAdapter.add(SettingsProfileItem(view.context.getString(R.string.notOnline)) {
                    Auth().logout(view.context)
                })
            }
            else -> {
                itemAdapter.add(SettingsLoginItem { username, password ->
                    val settings = Settings.getSettings(view.context)
                    settings.setString(view.context.getString(R.string.emailSetting), username)
                    settings.setString(view.context.getString(R.string.passwordSetting), password)

                    Auth().login(view.context, username, password, {
                        displayInfo(
                            view.context,
                            view.context.getString(R.string.loginSuccessfulMsg)
                        )
                    }, {
                        displayInfo(view.context, view.context.getString(R.string.loginFailedMsg))
                    })
                })
            }
        }
    }

    private fun addAdvancedSettings(view: View) {
        val context = view.context
        val settings = Settings.getSettings(context)

        itemAdapter.add(
            SettingsLabelItem(
                view.context.getString(R.string.advancedSettings)
            )
        )

        val changeSetting: (setting: String, value: Boolean, switch: SwitchMaterial) -> Boolean =
            { setting, value, _ ->
                settings.setBoolean(setting, value)
                value
            }

        itemAdapter.add(
            SettingsToggleItem(
                view.context.getString(R.string.disableAudioFocusSetting),
                true,
                view.context.getString(R.string.disableAudioFocusSwitch),
                null,
                changeSetting
            )
        )

        itemAdapter.add(
            SettingsToggleItem(
                context.getString(R.string.saveCoverImagesToCacheSetting),
                true,
                context.getString(R.string.saveCoverImagesToCache),
                null,
                changeSetting
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            itemAdapter.add(
                SettingsToggleItem(
                    context.getString(R.string.darkThemeSetting),
                    true,
                    context.getString(R.string.darkThemeSwitch),
                    null
                ) { setting, value, _ ->
                    settings.setBoolean(setting, value)

                    if (value) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }

                    return@SettingsToggleItem value
                })
        }

        itemAdapter.add(
            SettingsToggleItem(
                context.getString(R.string.downloadTypeSetting),
                "flac",
                context.getString(R.string.downloadFlacInsteadMp3),
                null
            ) { setting, value, _ ->
                if (value) {
                    settings.setString(setting, "flac")
                } else {
                    settings.setString(setting, "mp3_320")
                }

                return@SettingsToggleItem value
            })

        itemAdapter.add(
            SettingsToggleItem(
                context.getString(R.string.useCustomApiForCoverImagesSetting),
                true,
                context.getString(R.string.useCustomApiForCoverImages),
                context.getString(R.string.customApiSupportsV1Setting), changeSetting
            )
        )

        itemAdapter.add(
            SettingsToggleItem(
                context.getString(R.string.useCustomApiForSearchSetting),
                true,
                context.getString(R.string.useCustomApiForSearch),
                context.getString(R.string.customApiSupportsV1Setting), changeSetting
            )
        )

        itemAdapter.add(
            SettingsToggleItem(
                context.getString(R.string.useCustomApiForCatalogAndAlbumViewSetting),
                true,
                context.getString(R.string.useCustomApi),
                context.getString(R.string.customApiSupportsV1Setting), changeSetting
            )
        )

        itemAdapter.add(
            SettingsToggleItem(
                context.getString(R.string.useCustomApiForEverythingSetting),
                true,
                context.getString(R.string.useCustomApiForEverything),
                context.getString(R.string.customApiSupportsV2Setting)
            ) { setting, value, switch ->
                if (value) {
                    //check for custom api features

                    genericScope.launch {
                        checkCustomApiFeatures(context, {
                            switch.isChecked = true
                            settings.setBoolean(setting, true)
                            displaySnackBar(
                                view,
                                context.getString(R.string.customApiEnabledMsg),
                                null
                            ) {}

                        }, {
                            switch.isChecked = false
                            settings.setBoolean(setting, false)
                            displaySnackBar(
                                view,
                                context.getString(R.string.customApiEnableError),
                                null
                            ) {}

                        })
                    }

                    true
                } else {
                    settings.setBoolean(setting, false)
                    false
                }
            })
    }

    private fun addCoverResolutionSeekBar(view: View) {
        val settings = Settings.getSettings(view.context)

        settings.getInt(view.context.getString(R.string.primaryCoverResolutionSetting))
            ?.let { orig ->
                itemAdapter.add(
                    SettingsSeekBarItem(
                        view.context.getString(R.string.coverResolution),
                        2048 / 256,
                        orig / 256,
                        orig
                    ) { value, shownValueView ->
                        if (value != 0) {
                            settings.setInt(
                                view.context.getString(R.string.primaryCoverResolutionSetting),
                                (value * 256)
                            )
                            settings.setInt(
                                view.context.getString(R.string.secondaryCoverResolutionSetting),
                                (((value) * 256) / 4)
                            )
                        } else {
                            settings.setInt(
                                view.context.getString(R.string.primaryCoverResolutionSetting),
                                (128)
                            )
                            settings.setInt(
                                view.context.getString(R.string.secondaryCoverResolutionSetting),
                                (64)
                            )
                        }

                        settings.getInt(view.context.getString(R.string.primaryCoverResolutionSetting))
                            ?.let { shownValueView.text = it.toString() }
                    })
            }
    }

    private fun addVolumeSeekBar(view: View) {
        val settings = Settings.getSettings(view.context)

        settings.getFloat(view.context.getString(R.string.volumeSetting))?.let { orig ->
            volume = 1 - log(100 - (orig * 100), 100F)

            itemAdapter.add(
                SettingsSeekBarItem(
                    view.context.getString(R.string.volume),
                    100,
                    (orig * 100).toInt(),
                    (orig * 100).toInt()
                ) { value, shownValueView ->
                    settings.setFloat(
                        view.context.getString(R.string.volumeSetting),
                        (value.toFloat() / 100F)
                    )

                    settings.getFloat(view.context.getString(R.string.volumeSetting))
                        ?.let {
                            shownValueView.text = ((it * 100).toInt()).toString()
                            volume = 1 - log(100 - (it * 100), 100F)
                        }
                })
        }
    }

    private fun addCrossFadeSeekBar(view: View) {
        val settings = Settings.getSettings(view.context)

        settings.getInt(view.context.getString(R.string.crossfadeTimeSetting))?.let { orig ->
            crossfade = orig

            itemAdapter.add(
                SettingsSeekBarItem(
                    view.context.getString(R.string.crossfade),
                    20000 / 1000,
                    orig / 1000,
                    orig / 1000
                ) { value, shownValueView ->
                    shownValueView.text = value.toString()

                    settings.setInt(
                        view.context.getString(R.string.crossfadeTimeSetting),
                        (value * 1000)
                    )

                    settings.getInt(view.context.getString(R.string.crossfadeTimeSetting))
                        ?.let {
                            shownValueView.text = (it / 1000).toString()
                            crossfade = it
                        }
                })
        }
    }
}