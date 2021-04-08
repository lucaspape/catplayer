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
import de.lucaspape.monstercat.ui.showInformation
import de.lucaspape.monstercat.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log

class ToggleItem(override val typeId: String?, override val itemId: ToggleData) :
    Item(typeId, itemId)

class ToggleData(
    val checkValue: () -> Any?,
    val trueValue: Any,
    val itemText: String,
    val requiredApiFeature: String?,
    val onSwitchChange: (value: Boolean) -> Boolean,
    val description: String?
)

class ButtonItem(override val typeId: String?, override val itemId: ButtonData) :
    Item(typeId, itemId)

class ButtonData(val text: String, val onButtonPress: () -> Unit)

class SeekbarItem(override val typeId: String?, override val itemId: SeekbarData) :
    Item(typeId, itemId)

class SeekbarData(
    val title: String,
    val max: Int,
    val value: Int,
    val shownStart: Int,
    val onChange: (value: Int, shownValueView: TextView) -> Unit
)

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
            is ToggleItem -> {
                return if (item.typeId == "toggle") {
                    SettingsToggleItem(
                        item.itemId.checkValue,
                        item.itemId.trueValue,
                        item.itemId.itemText,
                        item.itemId.requiredApiFeature,
                        item.itemId.onSwitchChange,
                        item.itemId.description
                    )
                } else {
                    null
                }
            }
            is ButtonItem -> {
                return if (item.typeId == "button") {
                    SettingsButtonItem(item.itemId.text, item.itemId.onButtonPress)
                } else {
                    null
                }
            }
            is SeekbarItem -> {
                return if (item.typeId == "seekbar") {
                    SettingsSeekBarItem(
                        item.itemId.title,
                        item.itemId.max,
                        item.itemId.value,
                        item.itemId.shownStart,
                        item.itemId.onChange
                    )
                } else {
                    null
                }
            }
            else -> {
                null
            }
        }
    }

    override suspend fun onMenuButtonClick(
        view: View,
        viewData: List<GenericItem>,
        itemIndex: Int
    ) {
        super.onMenuButtonClick(view, viewData, itemIndex)

        val item = viewData[itemIndex]

        if(item is SettingsToggleItem && item.description != null){
            withContext(Dispatchers.Main){
                showInformation(view, item.description)
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

    private fun getLogin(context: Context): Item {
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

    private suspend fun getPlaybackSettings(context: Context): ArrayList<Item> {
        val content = ArrayList<Item>()

        content.add(ToggleItem("toggle", ToggleData({
            var filterExists = false

            FilterDatabaseHelper(context).getAllFilters().forEach {
                if (it.filterType == "special" && it.filter == "non-creator-friendly") {
                    filterExists = true
                }
            }

            filterExists
        }, true, context.getString(R.string.dontPlayNotCreatorFriendly), null, { value: Boolean ->
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
        }, context.getString(R.string.skipCreatorFriendlyDescription))))

        content.add(ToggleItem("toggle", ToggleData({
            var filterExists = false

            FilterDatabaseHelper(context).getAllFilters().forEach {
                if (it.filterType == "artist" && it.filter.equals("monstercat", ignoreCase = true)) {
                    filterExists = true
                }
            }

            filterExists
        }, true, context.getString(R.string.skipSongsMonstercat), null, { value: Boolean ->
            if (value) {
                FilterDatabaseHelper(context).insertFilter("artist", "monstercat")
            } else {
                val removeIds = ArrayList<Int>()

                FilterDatabaseHelper(context).getAllFilters().forEach {
                    if (it.filterType == "artist" && it.filter.equals("monstercat", ignoreCase = true)) {
                        removeIds.add(it.id)
                    }
                }

                removeIds.forEach {
                    FilterDatabaseHelper(context).removeFilter(it)
                }
            }

            applyFilterSettings(context)

            value
        }, context.getString(R.string.skipMonstercatDescription))))

        content.add(ToggleItem("toggle", ToggleData({
            var filterExists = false

            FilterDatabaseHelper(context).getAllFilters().forEach {
                if (it.filterType == "special" && it.filter == "explicit") {
                    filterExists = true
                }
            }

            filterExists
        }, true, context.getString(R.string.skipExplicitSongs), null, { value: Boolean ->
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
        }, context.getString(R.string.skipExplicitDescription))))

        val settings = Settings.getSettings(context)


        content.add(ToggleItem("toggle", ToggleData({
            settings.getBoolean(context.getString(R.string.hideToBeSkippedSetting))
        }, true, context.getString(R.string.hideToBeSkipped), null, { value: Boolean ->
            settings.setBoolean(context.getString(R.string.hideToBeSkippedSetting), value)
            applyFilterSettings(context)
            value
        }, context.getString(R.string.hideToBeSkippedDescription))))

        content.add(ButtonItem("button", ButtonData("Adjust Filters", openFilterSettings)))

        content.add(
            ToggleItem(
                "toggle", ToggleData(
                    {
                        settings.getBoolean(context.getString(R.string.playRelatedSetting))
                    },
                    true,
                    context.getString(R.string.playRelatedAfter),
                    context.getString(R.string.customApiSupportsPlayingRelatedSongsSetting),
                    { value: Boolean ->
                        playRelatedSongsAfterPlaylistFinished = value
                        settings.setBoolean(context.getString(R.string.playRelatedSetting), value)
                        value
                    }, context.getString(R.string.playRelatedDescription))
            )
        )

        //volume seekbar

        settings.getFloat(context.getString(R.string.volumeSetting))
            ?.let { orig ->
                withContext(Dispatchers.Main) {
                    volume = 1 - log(100 - (orig * 100), 100F)
                }

                content.add(
                    SeekbarItem(
                        "seekbar",
                        SeekbarData(
                            context.getString(R.string.volume),
                            100,
                            (orig * 100).toInt(),
                            (orig * 100).toInt()
                        ) { value: Int, shownValueView: TextView ->
                            settings.setFloat(
                                context.getString(R.string.volumeSetting),
                                (value.toFloat() / 100F)
                            )

                            settings.getFloat(context.getString(R.string.volumeSetting))
                                ?.let {
                                    shownValueView.text = ((it * 100).toInt()).toString()
                                    volume = 1 - log(100 - (it * 100), 100F)
                                }
                        })
                )
            }

        settings.getInt(context.getString(R.string.crossfadeTimeSetting))
            ?.let { orig ->
                crossfade = orig

                content.add(
                    SeekbarItem(
                        "seekbar",
                        SeekbarData(
                            context.getString(R.string.crossfade),
                            20000 / 1000,
                            orig / 1000,
                            orig / 1000
                        ) { value: Int, shownValueView: TextView ->
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
                        })
                )
            }

        return content
    }

    private fun getDataSettings(context: Context): ArrayList<Item> {
        val settings = Settings.getSettings(context)

        val content = ArrayList<Item>()

        content.add(ToggleItem("toggle", ToggleData({
            settings.getBoolean(context.getString(R.string.streamOverMobileSetting))
        }, true, context.getString(R.string.allowStreamMobile), null, { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.streamOverMobileSetting),
                value
            )
            value
        }, null)))

        content.add(ToggleItem("toggle", ToggleData({
            settings.getBoolean(context.getString(R.string.downloadOverMobileSetting))
        }, true, context.getString(R.string.allowDownloadMobile), null, { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.downloadOverMobileSetting),
                value
            )
            value
        }, null)))

        content.add(ToggleItem("toggle", ToggleData({
            settings.getString(context.getString(R.string.downloadCoversOverMobileSetting))
        }, true, context.getString(R.string.allowCoverDownloadMobile), null, { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.downloadCoversOverMobileSetting),
                value
            )
            value
        },null)))

        settings.getInt(context.getString(R.string.primaryCoverResolutionSetting))
            ?.let { orig ->
                content.add(
                    SeekbarItem(
                        "seekbar",
                        SeekbarData(
                            context.getString(R.string.coverResolution),
                            2048 / 256,
                            orig / 256,
                            orig
                        ) { value: Int, shownValueView: TextView ->
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
                        })
                )
            }

        return content
    }

    private fun getAdvancedSettings(context: Context): ArrayList<Item> {
        val settings = Settings.getSettings(context)

        val content = ArrayList<Item>()

        content.add(ToggleItem("toggle", ToggleData({
            settings.getBoolean(context.getString(R.string.disableAudioFocusSetting))
        }, true, context.getString(R.string.disableAudioFocusSwitch), null, { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.disableAudioFocusSetting),
                value
            )
            value
        }, context.getString(R.string.disableAudioFocusDescription))))

        content.add(ToggleItem("toggle", ToggleData({
            settings.getBoolean(context.getString(R.string.saveCoverImagesToCacheSetting))
        }, true, context.getString(R.string.saveCoverImagesToCache), null, { value: Boolean ->
            settings.setBoolean(
                context.getString(R.string.saveCoverImagesToCacheSetting),
                value
            )
            value
        }, context.getString(R.string.saveCoverImagesToCacheDescription))))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            content.add(ToggleItem("toggle", ToggleData({
                settings.getBoolean(context.getString(R.string.darkThemeSetting))
            }, true, context.getString(R.string.darkThemeSwitch), null, { value: Boolean ->
                settings.setBoolean(context.getString(R.string.darkThemeSetting), value)

                if (value) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }

                value
            }, null)))
        }

        content.add(ToggleItem("toggle", ToggleData({
            settings.getString(context.getString(R.string.downloadTypeSetting))
        }, "flac", context.getString(R.string.downloadFlacInsteadMp3), null, { value: Boolean ->
            if (value) {
                settings.setString(context.getString(R.string.downloadTypeSetting), "flac")
            } else {
                settings.setString(context.getString(R.string.downloadTypeSetting), "mp3_320")
            }

            value
        }, null)))

        content.add(
            ToggleItem(
                "toggle", ToggleData(
                    {
                        settings.getBoolean(context.getString(R.string.useCustomApiForCoverImagesSetting))
                    },
                    true,
                    context.getString(R.string.useCustomApiForCoverImages),
                    context.getString(R.string.customApiSupportsV1Setting),
                    { value: Boolean ->
                        settings.setBoolean(
                            context.getString(R.string.useCustomApiForCoverImagesSetting),
                            value
                        )
                        value
                    }, context.getString(R.string.useCustomApiForCoverDownloadDescription))
            )
        )

        content.add(
            ToggleItem(
                "toggle", ToggleData(
                    {
                        settings.getBoolean(context.getString(R.string.useCustomApiForSearchSetting))
                    },
                    true,
                    context.getString(R.string.useCustomApiForSearch),
                    context.getString(R.string.customApiSupportsV1Setting),
                    { value: Boolean ->
                        settings.setBoolean(
                            context.getString(R.string.useCustomApiForSearchSetting),
                            value
                        )
                        value
                    }, context.getString(R.string.useCustomApiForSearchDescription))
            )
        )

        content.add(
            ToggleItem(
                "toggle", ToggleData(
                    {
                        settings.getBoolean(context.getString(R.string.useCustomApiForCatalogAndAlbumViewSetting))
                    },
                    true,
                    context.getString(R.string.useCustomApi),
                    context.getString(R.string.customApiSupportsV1Setting),
                    { value: Boolean ->
                        settings.setBoolean(
                            context.getString(R.string.useCustomApiForCatalogAndAlbumViewSetting),
                            value
                        )
                        value
                    }, context.getString(R.string.useCustomApiDescription))
            )
        )

        content.add(ButtonItem("button", ButtonData(context.getString(R.string.resetDatabase)) {
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
        }))

        return content
    }
}