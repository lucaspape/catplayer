package de.lucaspape.monstercat.handlers

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.loadContinuousSongListAsyncTask
import de.lucaspape.monstercat.handlers.util.LoadContinuousSongListAsync
import de.lucaspape.monstercat.handlers.util.LoadPlaylistAsync
import de.lucaspape.monstercat.handlers.util.LoadPlaylistTracksAsync
import de.lucaspape.monstercat.handlers.util.playSongFromId
import de.lucaspape.monstercat.music.clearContinuous
import java.lang.ref.WeakReference

class PlaylistHandler {
    companion object {
        @JvmStatic
        var currentListViewData = ArrayList<HashMap<String, Any?>>()
        @JvmStatic
        var listViewDataIsPlaylistView = true

        @JvmStatic
        private var simpleAdapter: SimpleAdapter? = null

        private var currentPlaylistId: String? = null

        /**
         * Updates listView content
         */
        @JvmStatic
        fun updateListView(view: View) {
            val playlistList = view.findViewById<ListView>(R.id.playlistView)

            var from = arrayOf("shownTitle", "secondaryImage")
            var to = arrayOf(R.id.title, R.id.cover)

            if (listViewDataIsPlaylistView) {
                from = arrayOf("playlistName", "coverUrl")
                to = arrayOf(R.id.title, R.id.cover)
            }

            simpleAdapter = SimpleAdapter(
                view.context,
                currentListViewData,
                R.layout.list_single,
                from,
                to.toIntArray()
            )

            playlistList.adapter = simpleAdapter

        }

        @JvmStatic
        fun redrawListView(view: View) {
            val playlistList = view.findViewById<ListView>(R.id.playlistView)
            simpleAdapter!!.notifyDataSetChanged()
            playlistList.invalidateViews()
            playlistList.refreshDrawableState()
        }
    }

    /**
     * Update listView every second (for album covers)
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
     * Register listeners (buttons etc)
     */
    fun registerListeners(view: View) {
        //click on list
        val playlistList = view.findViewById<ListView>(R.id.playlistView)

        //refresh
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.playlistSwipeRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            if (listViewDataIsPlaylistView) {
                loadPlaylist(view, true, true)
            } else {
                loadPlaylistTracks(view, true, currentPlaylistId!!)
            }
        }

        playlistList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val itemValue = playlistList.getItemAtPosition(position) as HashMap<String, Any?>

            if (listViewDataIsPlaylistView) {
                currentPlaylistId = itemValue["playlistId"] as String
                loadPlaylistTracks(view, false, currentPlaylistId!!)
            } else {
                clearContinuous()

                val songId = itemValue["id"] as String
                playSongFromId(view.context, songId, true)

                val continuousList = ArrayList<String>()

                for (i in (position + 1 until playlistList.adapter.count)) {
                    val nextItemValue = playlistList.getItemAtPosition(i) as HashMap<String, Any?>
                    continuousList.add(nextItemValue["id"] as String)
                }

                loadContinuousSongListAsyncTask =
                    LoadContinuousSongListAsync(continuousList, WeakReference(view.context))
                loadContinuousSongListAsyncTask!!.executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR
                )
            }
        }
    }

    /**
     * Load playlists
     */
    fun loadPlaylist(view: View, forceReload: Boolean, showAfter: Boolean) {
        val contextReference = WeakReference<Context>(view.context)
        val viewReference = WeakReference<View>(view)
        LoadPlaylistAsync(
            viewReference,
            contextReference,
            forceReload,
            showAfter
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * Load the tracks from a playlist
     */
    private fun loadPlaylistTracks(
        view: View,
        forceReload: Boolean,
        playlistId: String
    ) {
        val contextReference = WeakReference<Context>(view.context)
        val viewReference = WeakReference<View>(view)
        LoadPlaylistTracksAsync(
            viewReference,
            contextReference,
            forceReload,
            playlistId
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}