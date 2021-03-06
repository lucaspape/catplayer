package de.lucaspape.monstercat.ui.pages

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.pages.util.Page
import de.lucaspape.util.CustomSpinnerClass
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.ui.pages.recycler.HomeAlbumRecyclerPage
import de.lucaspape.monstercat.ui.pages.recycler.HomeCatalogAlbumRecyclerPage
import de.lucaspape.monstercat.ui.pages.recycler.HomeCatalogRecyclerPage
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage

class HomePage(
    private val onSearch: (searchString: String?) -> Unit,
    private val openSettings: () -> Unit,
    private val openQueue: () -> Unit,
    private var albumMcId: String?,
    private var resetPosition: Boolean
) :
    Page() {

    constructor() : this({}, {}, {},null, false)

    companion object {
        @JvmStatic
        var addSongsTaskId = ""

        @JvmStatic
        val homePageName = "home"
    }

    private var homePageObject: RecyclerViewPage? = null
    override val layout: Int = R.layout.fragment_home

    private var resetData = false
    
    private var exitOnAppButton = false

    override fun onBackPressed(view: View): Boolean {
        return if(!exitOnAppButton){
            resetData = true
            setupSpinner(view)

            resetPosition = true

            if (!registerListeners(view)) {
                catalogView(view)
            } else {
                albumView(view)
            }

            exitOnAppButton = true
            
            false
        }else{
            true
        }
    }

    override fun onPause(view: View) {
        homePageObject?.saveRecyclerViewPosition(view.context)
    }

    override fun onCreate(view: View) {
        setupSpinner(view)

        val id = albumMcId

        if (!registerListeners(view) && albumMcId == null) {
            catalogView(view)
        } else if (id != null) {
            openAlbum(view, null, id)
            albumMcId = null
        } else {
            albumView(view)
        }
    }

    override val pageName: String = homePageName

    private fun catalogView(view: View) {
        homePageObject?.saveRecyclerViewPosition(view.context)

        homePageObject = HomeCatalogRecyclerPage()

        if (resetPosition) {
            homePageObject?.resetRecyclerViewSavedPosition(view.context)
            homePageObject?.resetSaveData()
        }

        resetPosition = false

        homePageObject?.onCreate(view)
    }

    private fun openAlbum(view: View, albumId: String?, albumMcId: String) {
        homePageObject?.saveRecyclerViewPosition(view.context)
        homePageObject =
            HomeCatalogAlbumRecyclerPage(
                albumId,
                albumMcId
            )

        if (resetPosition) {
            homePageObject?.resetRecyclerViewSavedPosition(view.context)
            homePageObject?.resetSaveData()
        }

        resetPosition = false

        if (resetData) {
            homePageObject?.resetSaveData()
        }

        homePageObject?.onCreate(view)

        exitOnAppButton = false
    }

    private fun albumView(view: View) {
        homePageObject?.saveRecyclerViewPosition(view.context)
        homePageObject =
            HomeAlbumRecyclerPage { albumId, albumMcId ->
                openAlbum(view, albumId, albumMcId)
            }

        if (resetPosition) {
            homePageObject?.resetRecyclerViewSavedPosition(view.context)
            homePageObject?.resetSaveData()
        }

        if (resetData) {
            homePageObject?.resetSaveData()
        }

        homePageObject?.onCreate(view)
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

        view.findViewById<ImageButton>(R.id.searchButton).setOnClickListener {
            onSearch(null)
        }

        view.findViewById<ImageButton>(R.id.queueButton).setOnClickListener {
            openQueue()
        }

        return albumViewSelected == true
    }
}