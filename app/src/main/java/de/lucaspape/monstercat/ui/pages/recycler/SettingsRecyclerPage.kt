package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.*
import de.lucaspape.monstercat.core.music.applyFilterSettings
import de.lucaspape.monstercat.core.music.crossfade
import de.lucaspape.monstercat.core.music.playRelatedSongsAfterPlaylistFinished
import de.lucaspape.monstercat.core.music.save.PlayerSaveState
import de.lucaspape.monstercat.core.music.volume
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.ui.abstract_items.settings.*
import de.lucaspape.monstercat.ui.activities.MainActivity
import de.lucaspape.monstercat.ui.displayInfo
import de.lucaspape.monstercat.ui.pages.util.Item
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import de.lucaspape.monstercat.ui.pages.util.StringItem
import de.lucaspape.monstercat.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log

class MapItem(override val typeId: String?, override val itemId: HashMap<String, Any?>) :
    Item(typeId, itemId)

class SettingsRecyclerPage(private val openFilterSettings: () -> Unit) : RecyclerViewPage() {
    override suspend fun itemToAbstractItem(view: View, item: Item): GenericItem? {
        return when (item) {
            is StringItem -> {
                when (item.typeId) {
                    "label" -> SettingsLabelItem(item.itemId)
                    "settings-profile-item" -> SettingsProfileItem(item.itemId) {
                        Auth().logout(view.context)
                    }
                    "settings-login-item" -> SettingsLoginItem { username, password ->
                        val settings = Settings.getSettings(view.context)
                        settings.setString(view.context.getString(R.string.emailSetting), username)
                        settings.setString(
                            view.context.getString(R.string.passwordSetting),
                            password
                        )

                        Auth().login(view.context, username, password, {
                            displayInfo(
                                view.context,
                                view.context.getString(R.string.loginSuccessfulMsg)
                            )
                        }, {
                            displayInfo(
                                view.context,
                                view.context.getString(R.string.loginFailedMsg)
                            )
                        })
                    }
                    else -> null
                }
            }
            is MapItem -> {
                return when (item.typeId) {
                    "toggle" -> {
                        val checkValue = item.itemId["checkValue"] as () -> Any?
                        val trueValue = item.itemId["trueValue"]!!
                        val itemText = item.itemId["itemText"] as String
                        val requiredApiFeature = item.itemId["requiredApiFeature"] as String?
                        val onSwitchChange = item.itemId["onSwitchChange"] as (Boolean) -> Boolean

                        SettingsToggleItem(
                            checkValue,
                            trueValue,
                            itemText,
                            requiredApiFeature,
                            onSwitchChange
                        )
                    }
                    "button" -> {
                        val text = item.itemId["text"] as String
                        val onButtonPress = item.itemId["onButtonPress"] as () -> Unit

                        SettingsButtonItem(text, onButtonPress)
                    }
                    "seekbar" -> {
                        val title = item.itemId["title"] as String
                        val max = item.itemId["max"] as Int
                        val value = item.itemId["value"] as Int
                        val shownStart = item.itemId["shownStart"] as Int
                        val onChange = item.itemId["onChange"] as (Int, TextView) -> Unit

                        SettingsSeekBarItem(title, max, value, shownStart, onChange)
                    }
                    else -> {
                        null
                    }
                }
            }
            else -> {
                null
            }
        }
    }

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemList: ArrayList<Item>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        if (skip == 0) {
            val content = ArrayList<Item>()

            content.add(StringItem("label", ""))

            //LOGIN

            content.add(StringItem("label", context.getString(R.string.accountSettings)))

            content.add(getLogin(context))

            //playback settings

            content.add(StringItem("label", context.getString(R.string.playbackSettings)))

            content += getPlaybackSettings(context)

            //data settings

            content.add(StringItem("label", context.getString(R.string.dataSettings)))

            content += getDataSettings(context)

            //advanced settings

            content.add(StringItem("label", context.getString(R.string.advancedSettings)))

            content += getAdvancedSettings(context)

            callback(content)
        } else {
            callback(ArrayList())
        }
    }

    private fun getLogin(context: Context):Item{
        return when {
            loggedIn -> {
                StringItem("settings-profile-item", username)
            }
            waitingForLogin || offline -> {

                    StringItem(
                        "settings-profile-item",
                        context.getString(R.string.notOnline)
                    )

            }
            else -> {
                StringItem("settings-login-item", "")
            }
        }
    }

    private suspend fun getPlaybackSettings(context: Context):ArrayList<Item>{
        val content = ArrayList<Item>()

        val skipNonCreatorFriendlySongsMap = HashMap<String, Any?>()

        skipNonCreatorFriendlySongsMap["checkValue"] = {
            var filterExists = false

            FilterDatabaseHelper(context).getAllFilters().forEach {
                if (it.filterType == "special" && it.filter == "non-creator-friendly") {
                    filterExists = true
                }
            }

            filterExists
        }

        skipNonCreatorFriendlySongsMap["trueValue"] = true
        skipNonCreatorFriendlySongsMap["itemText"] =
            context.getString(R.string.dontPlayNotCreatorFriendly)
        skipNonCreatorFriendlySongsMap["requiredApiFeature"] = null
        skipNonCreatorFriendlySongsMap["onSwitchChange"] = { value: Boolean ->
            if (value) {
                FilterDatabaseHelper(context).insertFilter(
                    "special",
                    "non-creator-friendly"
                )
            } else {
                val removeIds = ArrayList<Int>()

                FilterDatabaseHelper(context).getAllFilters().forEach {
                    if (it.filterType == "special" && it.filter == "non-creator-friendly") {
                        removeIds.add(it.id)
                    }
                }

                removeIds.forEach {
                    FilterDatabaseHelper(context).removeFilter(it)
                }
            }

            applyFilterSettings(context)

            value
        }

        content.add(MapItem("toggle", skipNonCreatorFriendlySongsMap))

        val skipSongsFromMonstercatMap = HashMap<String, Any?>()

        skipSongsFromMonstercatMap["checkValue"] = {
            var filterExists = false

            FilterDatabaseHelper(context).getAllFilters().forEach {
                if (it.filterType == "artist" && it.filter == "monstercat") {
                    filterExists = true
                }
            }

            filterExists
        }

        skipSongsFromMonstercatMap["trueValue"] = true
        skipSongsFromMonstercatMap["itemText"] = context.getString(R.string.skipSongsMonstercat)
        skipSongsFromMonstercatMap["requiredApiFeature"] = null
        skipSongsFromMonstercatMap["onSwitchChange"] = { value: Boolean ->
            if (value) {
                FilterDatabaseHelper(context).insertFilter("artist", "monstercat")
            } else {
                val removeIds = ArrayList<Int>()

                FilterDatabaseHelper(context).getAllFilters().forEach {
                    if (it.filterType == "artist" && it.filter == "monstercat") {
                        removeIds.add(it.id)
                    }
                }

                removeIds.forEach {
                    FilterDatabaseHelper(context).removeFilter(it)
                }
            }

            applyFilterSettings(context)

            value
        }

        content.add(MapItem("toggle", skipSongsFromMonstercatMap))

        val skipExplicitSongsMap = HashMap<String, Any?>()

        skipExplicitSongsMap["checkValue"] = {
            var filterExists = false

            FilterDatabaseHelper(context).getAllFilters().forEach {
                if (it.filterType == "special" && it.filter == "explicit") {
                    filterExists = true
                }
            }

            filterExists
        }

        skipExplicitSongsMap["trueValue"] = true
        skipExplicitSongsMap["itemText"] = context.getString(R.string.skipExplicitSongs)
        skipExplicitSongsMap["requiredApiFeature"] = null
        skipExplicitSongsMap["onSwitchChange"] = { value: Boolean ->
            if (value) {
                FilterDatabaseHelper(context).insertFilter("special", "explicit")
            } else {
                val removeIds = ArrayList<Int>()

                FilterDatabaseHelper(context).getAllFilters().forEach {
                    if (it.filterType == "special" && it.filter == "explicit") {
                        removeIds.add(it.id)
                    }
                }

                removeIds.forEach {
                    FilterDatabaseHelper(context).removeFilter(it)
                }
            }

            applyFilterSettings(context)

            value
        }

        content.add(MapItem("toggle", skipExplicitSongsMap))

        val settings = Settings.getSettings(context)

        val hideToBeSkippedMap = HashMap<String, Any?>()

        hideToBeSkippedMap["checkValue"] = {
            settings.getBoolean(context.getString(R.string.hideToBeSkippedSetting))
        }

        hideToBeSkippedMap["trueValue"] = true
        hideToBeSkippedMap["itemText"] = context.getString(R.string.hideToBeSkipped)
        hideToBeSkippedMap["requiredApiFeature"] = null
        hideToBeSkippedMap["onSwitchChange"] = { value: Boolean ->
            settings.setBoolean(context.getString(R.string.hideToBeSkippedSetting), value)
            applyFilterSettings(context)
            value
        }

        content.add(MapItem("toggle", hideToBeSkippedMap))

        val openFiltersButtonMap = HashMap<String, Any?>()

        openFiltersButtonMap["text"] = "Adjust filters"
        openFiltersButtonMap["onButtonPress"] = {
            openFilterSettings()
        }

        content.add(MapItem("button", openFiltersButtonMap))

        val playRelatedMap = HashMap<String, Any?>()

        playRelatedMap["checkValue"] = {
            settings.getBoolean(context.getString(R.string.playRelatedSetting))
        }

        playRelatedMap["trueValue"] = true
        playRelatedMap["itemText"] = context.getString(R.string.playRelatedAfter)
        playRelatedMap["requiredApiFeature"] =
            context.getString(R.string.customApiSupportsPlayingRelatedSongsSetting)
        playRelatedMap["onSwitchChange"] = { value: Boolean ->
            playRelatedSongsAfterPlaylistFinished = value
            settings.setBoolean(context.getString(R.string.playRelatedSetting), value)
            value
        }

        content.add(MapItem("toggle", playRelatedMap))

        //volume seekbar

        settings.getFloat(context.getString(R.string.volumeSetting))
            ?.let { orig ->
                withContext(Dispatchers.Main) {
                    volume = 1 - log(100 - (orig * 100), 100F)
                }

                val volumeSeekbarMap = HashMap<String, Any?>()

                volumeSeekbarMap["title"] = context.getString(R.string.volume)
                volumeSeekbarMap["max"] = 100
                volumeSeekbarMap["value"] = (orig * 100).toInt()
                volumeSeekbarMap["shownStart"] = (orig * 100).toInt()
                volumeSeekbarMap["onChange"] = { value: Int, shownValueView: TextView ->
                    settings.setFloat(
                        context.getString(R.string.volumeSetting),
                        (value.toFloat() / 100F)
                    )

                    settings.getFloat(context.getString(R.string.volumeSetting))
                        ?.let {
                            shownValueView.text = ((it * 100).toInt()).toString()
                            volume = 1 - log(100 - (it * 100), 100F)
                        }
                }

                content.add(MapItem("seekbar", volumeSeekbarMap))
            }

        settings.getInt(context.getString(R.string.crossfadeTimeSetting))
            ?.let { orig ->
                crossfade = orig

                val crossfadeMap = HashMap<String, Any?>()

                crossfadeMap["title"] = context.getString(R.string.crossfade)
                crossfadeMap["max"] = 20000 / 1000
                crossfadeMap["value"] = orig / 1000
                crossfadeMap["shownStart"] = orig / 1000
                crossfadeMap["onChange"] = { value: Int, shownValueView: TextView ->
                    shownValueView.text = value.toString()

                    settings.setInt(
                        context.getString(R.string.crossfadeTimeSetting),
                        (value * 1000)
                    )

                    settings.getInt(context.getString(R.string.crossfadeTimeSetting))
                        ?.let {
                            shownValueView.text = (it / 1000).toString()
                            crossfade = it
                        }
                }

                content.add(MapItem("seekbar", crossfadeMap))
            }

        return content
    }

    private fun getDataSettings(context: Context):ArrayList<Item>{
        val settings = Settings.getSettings(context)

        val content = ArrayList<Item>()

        val streamOverMobileMap = HashMap<String, Any?>()

        streamOverMobileMap["checkValue"] = {
            settings.getBoolean(context.getString(R.string.streamOverMobileSetting))
        }

        streamOverMobileMap["trueValue"] = true
        streamOverMobileMap["itemText"] = context.getString(R.string.allowStreamMobile)
        streamOverMobileMap["requiredApiFeature"] = null
        streamOverMobileMap["onSwitchChange"] = { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.streamOverMobileSetting),
                value
            )
            value
        }

        content.add(MapItem("toggle", streamOverMobileMap))

        val downloadOverMobileMap = HashMap<String, Any?>()

        downloadOverMobileMap["checkValue"] = {
            settings.getBoolean(context.getString(R.string.downloadOverMobileSetting))
        }

        downloadOverMobileMap["trueValue"] = true
        downloadOverMobileMap["itemText"] = context.getString(R.string.allowDownloadMobile)
        downloadOverMobileMap["requiredApiFeature"] = null
        downloadOverMobileMap["onSwitchChange"] = { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.downloadOverMobileSetting),
                value
            )
            value
        }

        content.add(MapItem("toggle", downloadOverMobileMap))

        val downloadCoversOverMobileMap = HashMap<String, Any?>()

        downloadCoversOverMobileMap["checkValue"] = {
            settings.getString(context.getString(R.string.downloadCoversOverMobileSetting))
        }

        downloadCoversOverMobileMap["trueValue"] = true
        downloadCoversOverMobileMap["itemText"] =
            context.getString(R.string.allowCoverDownloadMobile)
        downloadCoversOverMobileMap["requiredApiFeature"] = null
        downloadCoversOverMobileMap["onSwitchChange"] = { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.downloadCoversOverMobileSetting),
                value
            )
            value
        }

        content.add(MapItem("toggle", downloadCoversOverMobileMap))

        settings.getInt(context.getString(R.string.primaryCoverResolutionSetting))
            ?.let { orig ->
                val coverResolutionSeekbar = HashMap<String, Any?>()

                coverResolutionSeekbar["title"] = context.getString(R.string.coverResolution)
                coverResolutionSeekbar["max"] = 2048 / 256
                coverResolutionSeekbar["value"] = orig / 256
                coverResolutionSeekbar["shownStart"] = orig
                coverResolutionSeekbar["onChange"] = { value: Int, shownValueView: TextView ->
                    if (value != 0) {
                        settings.setInt(
                            context.getString(R.string.primaryCoverResolutionSetting),
                            (value * 256)
                        )
                        settings.setInt(
                            context.getString(R.string.secondaryCoverResolutionSetting),
                            (((value) * 256) / 4)
                        )
                    } else {
                        settings.setInt(
                            context.getString(R.string.primaryCoverResolutionSetting),
                            (128)
                        )
                        settings.setInt(
                            context.getString(R.string.secondaryCoverResolutionSetting),
                            (64)
                        )
                    }

                    settings.getInt(context.getString(R.string.primaryCoverResolutionSetting))
                        ?.let { shownValueView.text = it.toString() }
                }

                content.add(MapItem("seekbar", coverResolutionSeekbar))
            }

        return content
    }

    private fun getAdvancedSettings(context: Context):ArrayList<Item>{
        val settings = Settings.getSettings(context)

        val content = ArrayList<Item>()

        val disableAudioFocusMap = HashMap<String, Any?>()

        disableAudioFocusMap["checkValue"] = {
            settings.getBoolean(context.getString(R.string.disableAudioFocusSetting))
        }

        disableAudioFocusMap["trueValue"] = true
        disableAudioFocusMap["itemText"] =
            context.getString(R.string.disableAudioFocusSwitch)
        disableAudioFocusMap["requiredApiFeature"] = null
        disableAudioFocusMap["onSwitchChange"] = { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.disableAudioFocusSetting),
                value
            )
            value
        }

        content.add(MapItem("toggle", disableAudioFocusMap))

        val saveCoversToCacheMap = HashMap<String, Any?>()

        saveCoversToCacheMap["checkValue"] = {
            settings.getBoolean(context.getString(R.string.saveCoverImagesToCacheSetting))
        }

        saveCoversToCacheMap["trueValue"] = true
        saveCoversToCacheMap["itemText"] =
            context.getString(R.string.saveCoverImagesToCache)
        saveCoversToCacheMap["requiredApiFeature"] = null
        saveCoversToCacheMap["onSwitchChange"] = { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.saveCoverImagesToCacheSetting),
                value
            )
            value
        }

        content.add(MapItem("toggle", saveCoversToCacheMap))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val darkThemeMap = HashMap<String, Any?>()

            darkThemeMap["checkValue"] = {
                settings.getBoolean(context.getString(R.string.darkThemeSetting))
            }

            darkThemeMap["trueValue"] = true
            darkThemeMap["itemText"] =
                context.getString(R.string.darkThemeSwitch)
            darkThemeMap["requiredApiFeature"] = null
            darkThemeMap["onSwitchChange"] = { value: Boolean ->
                settings.setBoolean(context.getString(R.string.darkThemeSetting), value)

                if (value) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }

                value
            }

            content.add(MapItem("toggle", darkThemeMap))
        }

        val downloadTypeMap = HashMap<String, Any?>()

        downloadTypeMap["checkValue"] = {
            settings.getString(context.getString(R.string.downloadTypeSetting))
        }

        downloadTypeMap["trueValue"] = "flac"
        downloadTypeMap["itemText"] =
            context.getString(R.string.downloadFlacInsteadMp3)
        downloadTypeMap["requiredApiFeature"] = null
        downloadTypeMap["onSwitchChange"] = { value: Boolean ->
            if (value) {
                settings.setString(context.getString(R.string.downloadTypeSetting), "flac")
            } else {
                settings.setString(context.getString(R.string.downloadTypeSetting), "mp3_320")
            }

            value
        }

        content.add(MapItem("toggle", downloadTypeMap))

        val useCustomApiForCoverImagesMap = HashMap<String, Any?>()

        useCustomApiForCoverImagesMap["checkValue"] = {
            settings.getBoolean(context.getString(R.string.useCustomApiForCoverImagesSetting))
        }

        useCustomApiForCoverImagesMap["trueValue"] = true
        useCustomApiForCoverImagesMap["itemText"] =
            context.getString(R.string.useCustomApiForCoverImages)
        useCustomApiForCoverImagesMap["requiredApiFeature"] =
            context.getString(R.string.customApiSupportsV1Setting)
        useCustomApiForCoverImagesMap["onSwitchChange"] = { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.useCustomApiForCoverImagesSetting),
                value
            )
            value
        }

        content.add(MapItem("toggle", useCustomApiForCoverImagesMap))

        val useCustomApiForSearchMap = HashMap<String, Any?>()

        useCustomApiForSearchMap["checkValue"] = {
            settings.getBoolean(context.getString(R.string.useCustomApiForSearchSetting))
        }

        useCustomApiForSearchMap["trueValue"] = true
        useCustomApiForSearchMap["itemText"] =
            context.getString(R.string.useCustomApiForSearch)
        useCustomApiForSearchMap["requiredApiFeature"] =
            context.getString(R.string.customApiSupportsV1Setting)
        useCustomApiForSearchMap["onSwitchChange"] = { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.useCustomApiForSearchSetting),
                value
            )
            value
        }

        content.add(MapItem("toggle", useCustomApiForSearchMap))

        val useCustomApiMap = HashMap<String, Any?>()

        useCustomApiMap["checkValue"] = {
            settings.getBoolean(context.getString(R.string.useCustomApiForCatalogAndAlbumViewSetting))
        }

        useCustomApiMap["trueValue"] = true
        useCustomApiMap["itemText"] =
            context.getString(R.string.useCustomApi)
        useCustomApiMap["requiredApiFeature"] =
            context.getString(R.string.customApiSupportsV1Setting)
        useCustomApiMap["onSwitchChange"] = { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.useCustomApiForCatalogAndAlbumViewSetting),
                value
            )
            value
        }

        content.add(MapItem("toggle", useCustomApiMap))

        //reset database button

        val resetDatabaseButtonMap = HashMap<String, Any?>()

        resetDatabaseButtonMap["text"] = context.getString(R.string.resetDatabase)
        resetDatabaseButtonMap["onButtonPress"] = {
            val alertDialogBuilder = AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.resetDatabase))
                .setMessage(context.getString(R.string.resetDatabaseQuestion))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    AlbumDatabaseHelper(context).reCreateTable(context, true)
                    ItemDatabaseHelper(context, "catalog").reCreateTable()
                    PlaylistDatabaseHelper(context).reCreateTable(context, true)
                    ManualPlaylistDatabaseHelper(context).reCreateTable(context, true)

                    PlayerSaveState.delete(context)

                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)

                    Runtime.getRuntime().exit(0)
                }
                .setNegativeButton(android.R.string.cancel, null)

            val dialog = alertDialogBuilder.create()
            dialog.show()

            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setTextColor(typedValue.data)

            val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            negativeButton.setTextColor(typedValue.data)
        }

        content.add(MapItem("button", resetDatabaseButtonMap))

        return content
    }
}