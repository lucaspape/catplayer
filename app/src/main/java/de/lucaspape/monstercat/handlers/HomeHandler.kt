package de.lucaspape.monstercat.handlers

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.handlers.util.LoadAlbumAsync
import de.lucaspape.monstercat.handlers.util.LoadAlbumListAsync
import de.lucaspape.monstercat.handlers.util.LoadSongListAsync
import de.lucaspape.monstercat.handlers.util.playSongFromId
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.settings.Settings
import java.lang.ref.WeakReference

/**
 * Does everything for the home page
 */
class HomeHandler {

    companion object {
        @JvmStatic
        var albumViewSelected = false
        @JvmStatic
        var albumView = false
        @JvmStatic
        var currentListViewData = ArrayList<HashMap<String, Any?>>()
        @JvmStatic
        val loadMax = 200
        @JvmStatic
        private var simpleAdapter: SimpleAdapter? = null

        @JvmStatic
        fun redrawListView(view: View) {
            val musicList = view.findViewById<ListView>(R.id.musiclistview)
            simpleAdapter!!.notifyDataSetChanged()
            musicList.invalidateViews()
            musicList.refreshDrawableState()
        }

        /**
         * Updates content of listView
         */
        @JvmStatic
        fun updateListView(view: View) {
            val musicList = view.findViewById<ListView>(R.id.musiclistview)

            if (albumView) {
                val from = arrayOf("title", "primaryImage")
                val to = arrayOf(R.id.description, R.id.cover)
                simpleAdapter = SimpleAdapter(
                    view.context,
                    currentListViewData,
                    R.layout.list_album_view,
                    from,
                    to.toIntArray()
                )
                musicList.adapter = simpleAdapter
            } else {
                val from = arrayOf("shownTitle", "secondaryImage")
                val to = arrayOf(R.id.title, R.id.cover)
                simpleAdapter = SimpleAdapter(
                    view.context,
                    currentListViewData,
                    R.layout.list_single,
                    from,
                    to.toIntArray()
                )
                musicList.adapter = simpleAdapter
            }

        }
    }

    /**
     * Update listview every second (for album covers)
     */
    fun setupListView(view: View) {
        updateListView(view)
        redrawListView(view)

        //setup auto reload
        Thread(Runnable {
            while (true) {
                Handler(Looper.getMainLooper()).post { redrawListView(view) }
                Thread.sleep(1000)
            }

        }).start()
    }

    /**
     * Album/Catalog view selector
     */
    fun setupSpinner(view: View) {
        val viewSelector = view.findViewById<Spinner>(R.id.viewSelector)

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
     * Set the correct views for the MusicPlayer.kt
     */
    fun setupMusicPlayer(view: View) {
        val textview1 = view.findViewById<TextView>(R.id.songCurrent1)
        val textview2 = view.findViewById<TextView>(R.id.songCurrent2)
        val coverBarImageView = view.findViewById<ImageView>(R.id.barCoverImage)
        val musicToolBar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.muscicbar)
        val playButton = view.findViewById<ImageButton>(R.id.playButton)
        val seekBar = view.findViewById<SeekBar>(R.id.seekBar)

        val weakReference = WeakReference(view.context)

        //setup musicPlayer

        contextReference = (weakReference)
        setTextView(textview1, textview2)
        setSeekBar(seekBar)
        setBarCoverImageView(coverBarImageView)
        setMusicBar(musicToolBar)
        setPlayButton(playButton)
    }

    /**
     * Listeners (buttons, refresh etc)
     */
    fun registerListeners(view: View) {
        //refresh
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            if (albumView) {
                loadAlbumList(view, true)
            } else {
                loadSongList(view, true)
            }
        }

        //music control buttons
        val playButton = view.findViewById<ImageButton>(R.id.playButton)
        val backButton = view.findViewById<ImageButton>(R.id.backbutton)
        val nextButton = view.findViewById<ImageButton>(R.id.nextbutton)

        playButton.setOnClickListener {
            toggleMusic()
        }

        nextButton.setOnClickListener {
            next()
        }

        backButton.setOnClickListener {
            previous()
        }

        //click on list
        val musicList = view.findViewById<ListView>(R.id.musiclistview)
        musicList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (albumView) {
                val itemValue = musicList.getItemAtPosition(position) as HashMap<String, Any?>
                loadAlbum(view, itemValue, false)
            } else {
                val itemValue = musicList.getItemAtPosition(position) as HashMap<String, Any?>
                playSongFromId(
                    view.context,
                    itemValue["id"] as String,
                    true
                )
            }
        }

        val viewSelector = view.findViewById<Spinner>(R.id.viewSelector)

        if (albumViewSelected) {
            viewSelector.setSelection(1)
        } else {
            viewSelector.setSelection(0)
        }

        val settings = Settings(view.context)
        settings.saveSetting("albumViewSelected", albumViewSelected.toString())

        viewSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

                if (albumViewSelected) {
                    viewSelector.setSelection(1)
                } else {
                    viewSelector.setSelection(0)
                }

                updateListView(view)
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                v: View?,
                position: Int,
                id: Long
            ) {
                when {
                    viewSelector.getItemAtPosition(position) == "Catalog View" -> {
                        albumView = false
                        albumViewSelected = false
                        settings.saveSetting("albumViewSelected", albumViewSelected.toString())
                    }
                    viewSelector.getItemAtPosition(position) == "Album View" -> {
                        albumView = true
                        albumViewSelected = true
                        settings.saveSetting("albumViewSelected", albumViewSelected.toString())
                    }
                }

                if (albumView) {
                    loadAlbumList(view, false)
                } else {
                    loadSongList(view, false)
                }
            }
        }
    }

    /**
     * Load song list ("catalog view")
     */
    fun loadSongList(view: View, forceReload: Boolean) {
        val contextReference = WeakReference<Context>(view.context)
        val viewReference = WeakReference<View>(view)
        LoadSongListAsync(
            viewReference,
            contextReference,
            forceReload
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * Load album list ("album view")
     */
    fun loadAlbumList(view: View, forceReload: Boolean) {
        val contextReference = WeakReference<Context>(view.context)
        val viewReference = WeakReference<View>(view)
        LoadAlbumListAsync(
            viewReference,
            contextReference,
            forceReload
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * Load single album
     */
    private fun loadAlbum(view: View, itemValue: HashMap<String, Any?>, forceReload: Boolean) {
        val listView = view.findViewById<ListView>(R.id.musiclistview)

        val settings = Settings(view.context)
        settings.saveSetting("currentListAlbumViewLastScrollIndex", listView.firstVisiblePosition.toString())
        settings.saveSetting("currentListAlbumViewTop", (listView.getChildAt(0).top - listView.paddingTop).toString())

        val contextReference = WeakReference<Context>(view.context)
        val viewReference = WeakReference<View>(view)
        LoadAlbumAsync(
            viewReference,
            contextReference,
            forceReload,
            itemValue
        ).executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
    }
}