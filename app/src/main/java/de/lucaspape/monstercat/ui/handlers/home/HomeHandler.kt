package de.lucaspape.monstercat.ui.handlers.home

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.music.util.playStream
import de.lucaspape.monstercat.twitch.Stream
import de.lucaspape.monstercat.ui.handlers.Handler
import de.lucaspape.util.CustomSpinnerClass
import de.lucaspape.util.Settings

interface HomeHandlerInterface {
    fun saveRecyclerViewPosition(context: Context)
    fun onCreate(view: View)
    fun resetRecyclerViewSavedPosition(context: Context)
}

class HomeHandler(
    private val onSearch: (searchString: String?) -> Unit,
    private val openSettings: () -> Unit,
    private val albumMcId: String?,
    private val resetPosition: Boolean
) :
    Handler {

    companion object {
        @JvmStatic
        var addSongsTaskId = ""
    }

    private var homeHandlerObject: HomeHandlerInterface? = null
    override val layout: Int = R.layout.fragment_home

    override fun onBackPressed(view: View) {
        onCreate(view)
    }

    override fun onPause(view: View) {
        homeHandlerObject?.saveRecyclerViewPosition(view.context)
    }

    override fun onCreate(view: View) {
        setupSpinner(view)

        if (!registerListeners(view) && albumMcId == null) {
            catalogView(view)
        } else if (albumMcId != null) {
            openAlbum(view, null, albumMcId)
        } else {
            albumView(view)
        }
    }

    private fun catalogView(view: View) {
        homeHandlerObject?.saveRecyclerViewPosition(view.context)

        homeHandlerObject =
            HomeCatalogHandler(
                null,
                null
            )

        if (resetPosition)
            homeHandlerObject?.resetRecyclerViewSavedPosition(view.context)

        homeHandlerObject?.onCreate(view)
    }

    private fun openAlbum(view: View, albumId: String?, albumMcId: String) {
        homeHandlerObject?.saveRecyclerViewPosition(view.context)
        homeHandlerObject =
            HomeCatalogHandler(
                albumId,
                albumMcId
            )
        if (resetPosition)
            homeHandlerObject?.resetRecyclerViewSavedPosition(view.context)

        homeHandlerObject?.onCreate(view)
    }

    private fun albumView(view: View) {
        homeHandlerObject?.saveRecyclerViewPosition(view.context)
        homeHandlerObject =
            HomeAlbumHandler { albumId, albumMcId ->
                openAlbum(view, albumId, albumMcId)
            }

        if (resetPosition)
            homeHandlerObject?.resetRecyclerViewSavedPosition(view.context)

        homeHandlerObject?.onCreate(view)
    }

    /**
     * Album/Catalog view selector
     */
    private fun setupSpinner(view: View) {
        val viewSelector = view.findViewById<CustomSpinnerClass>(R.id.viewSelector)

        val selectorItems = arrayOf(
            view.context.getString(R.string.catalogView),
            view.context.getString(R.string.albumView)
        )
        val arrayAdapter = ArrayAdapter<Any?>(
            view.context,
            R.layout.support_simple_spinner_dropdown_item,
            selectorItems
        )

        viewSelector.adapter = arrayAdapter
    }

    /**
     * Listeners (buttons, refresh etc)
     */
    private fun registerListeners(view: View): Boolean {
        val viewSelector = view.findViewById<CustomSpinnerClass>(R.id.viewSelector)

        val settings = Settings.getSettings(view.context)

        var albumViewSelected =
            settings.getBoolean(view.context.getString(R.string.albumViewSelectedSetting))

        if (albumViewSelected == null) {
            albumViewSelected = false
        }

        if (albumViewSelected) {
            viewSelector.programmaticallySetPosition(1, false)
        } else {
            viewSelector.programmaticallySetPosition(0, false)
        }

        var selected = 0

        viewSelector.setOnItemSelectedListener(object : CustomSpinnerClass.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

                if (albumViewSelected == true) {
                    viewSelector.programmaticallySetPosition(1, false)
                } else {
                    viewSelector.programmaticallySetPosition(0, false)
                }
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                v: View?,
                position: Int,
                id: Long,
                userSelected: Boolean
            ) {
                //dont call on initial change, only if from user
                if (++selected > 1 && userSelected) {
                    when {
                        viewSelector.getItemAtPosition(position) == view.context.getString(R.string.catalogView) -> {
                            albumViewSelected = false

                            settings.setBoolean(
                                view.context.getString(R.string.albumViewSelectedSetting),
                                false
                            )

                            catalogView(view)
                        }
                        viewSelector.getItemAtPosition(position) == view.context.getString(R.string.albumView) -> {
                            albumViewSelected = true

                            settings.setBoolean(
                                view.context.getString(R.string.albumViewSelectedSetting),
                                true
                            )

                            albumView(view)
                        }
                    }
                }

            }
        })

        //settings button
        view.findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            openSettings()
        }

        //livestream button
        view.findViewById<ImageButton>(R.id.liveButton).setOnClickListener {
            playStream(
                view.context,
                Stream(
                    view.context.getString(R.string.twitchClientID),
                    view.context.getString(R.string.twitchChannel)
                )
            )
        }

        view.findViewById<ImageButton>(R.id.searchButton).setOnClickListener {
            onSearch(null)
        }

        return albumViewSelected == true
    }
}