package de.lucaspape.monstercat.handlers

import android.app.AlertDialog
import android.content.Context
import android.os.AsyncTask
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ListView
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.background.BackgroundService.Companion.loadContinuousSongListAsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.*
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.handlers.async.*
import de.lucaspape.monstercat.music.addSong
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.*
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference

/**
 * Play a song from ID
 */
internal fun playSongFromId(context: Context, songId: String, playNow: Boolean) {
    val songDatabaseHelper = SongDatabaseHelper(context)
    val song = songDatabaseHelper.getSong(context, songId)

    if (song != null) {
        if (playNow) {
            de.lucaspape.monstercat.music.playNow(song)
        } else {
            addSong(song)
        }

    } else {
        displayInfo(context, context.getString(R.string.couldNotFindSongMSg))
    }
}

/**
 * Play a song from ID with next songs (-> songs which are listed under the current song in listView)
 */
internal fun playSongFromId(
    context: Context,
    songId: String,
    playNow: Boolean,
    musicList: ListView,
    position: Int
) {
    playSongFromId(context, songId, playNow)

    val continuousList = ArrayList<String>()

    for (i in (position + 1 until musicList.adapter.count)) {
        val nextItemValue = musicList.getItemAtPosition(i) as HashMap<*, *>
        continuousList.add(nextItemValue["id"] as String)
    }

    loadContinuousSongListAsyncTask =
        LoadContinuousSongListAsync(
            continuousList,
            WeakReference(context)
        )

    loadContinuousSongListAsyncTask!!.executeOnExecutor(
        AsyncTask.THREAD_POOL_EXECUTOR
    )
}

/**
 * Play an entire album after the current song
 */
internal fun playAlbumNext(context: Context, mcID: String) {
    val requestUrl =
        context.getString(R.string.loadAlbumSongsUrl) + "/" + mcID

    val albumRequestQueue = Volley.newRequestQueue(context)

    val albumRequest = AuthorizedRequest(Request.Method.GET, requestUrl,
        getSid(),
        Response.Listener { response ->
            val jsonObject = JSONObject(response)
            val jsonArray = jsonObject.getJSONArray("tracks")

            val idArray = ArrayList<Long>()

            for (i in (0 until jsonArray.length())) {
                parseSongToDB(jsonArray.getJSONObject(i), context)?.let { id ->
                    idArray.add(id)
                }
            }

            val databaseHelper = SongDatabaseHelper(context)

            idArray.reverse()

            for (id in idArray) {
                addSong(databaseHelper.getSong(context, id))
            }
        },
        Response.ErrorListener {
            displayInfo(context, context.getString(R.string.errorRetrieveAlbumData))
        })

    albumRequestQueue.add(albumRequest)
}

/**
 * Download an entire playlist
 */
internal fun downloadPlaylist(context: Context, playlistId: String) {
    LoadPlaylistTracksAsync(WeakReference(context), true, playlistId) {
        val playlistItemDatabaseHelper =
            PlaylistItemDatabaseHelper(context, playlistId)
        val playlistItemList = playlistItemDatabaseHelper.getAllData()

        for (playlistItem in playlistItemList) {
            val songDatabaseHelper = SongDatabaseHelper(context)
            val song = songDatabaseHelper.getSong(context, playlistItem.songId)

            downloadSong(context, song)
        }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

/**
 * Download song
 */
internal fun downloadSong(context: Context, song: Song) {
    if (song.isDownloadable) {
        val settings = Settings(context)

        val downloadType = settings.getSetting("downloadType")
        val downloadQuality = settings.getSetting("downloadQuality")

        val downloadUrl =
            context.getString(R.string.trackContentUrl) + song.albumId + "/track-download/" + song.songId + "?format=" + downloadType + "_" + downloadQuality

        val downloadLocation =
            context.getExternalFilesDir(null).toString() + "/" + song.artist + song.title + song.version + "." + downloadType

        if (!File(downloadLocation).exists()) {
            val sSid = getSid()
            if (sSid != null) {
                addDownloadSong(downloadUrl, downloadLocation, song.title + " " + song.version)
            }

        } else {
            displayInfo(
                context,
                context.getString(R.string.alreadyDownloadedMsg, song.title + " " + song.version)
            )
        }
    } else {
        displayInfo(
            context,
            context.getString(R.string.downloadNotAvailableMsg, song.title + " " + song.version)
        )
    }
}

/**
 * Download an entire album
 */
internal fun downloadAlbum(context: Context, mcID: String) {
    val requestUrl =
        context.getString(R.string.loadAlbumSongsUrl) + "/" + mcID

    val albumRequestQueue = Volley.newRequestQueue(context)

    val albumRequest = AuthorizedRequest(Request.Method.GET, requestUrl,
        getSid(),
        Response.Listener { response ->
            val jsonObject = JSONObject(response)
            val jsonArray = jsonObject.getJSONArray("tracks")

            val idArray = ArrayList<Long>()

            for (i in (0 until jsonArray.length())) {
                parseSongToDB(jsonArray.getJSONObject(i), context)?.let { id ->
                    idArray.add(id)
                }
            }

            val databaseHelper = SongDatabaseHelper(context)

            for (id in idArray) {
                val song = databaseHelper.getSong(context, id)
                downloadSong(context, song)
            }
        },
        Response.ErrorListener {
            displayInfo(context, context.getString(R.string.errorRetrieveAlbumData))
        })

    albumRequestQueue.add(albumRequest)
}

/**
 * Add single song to playlist, will ask for playlist with alertDialog
 */
internal fun addSongToPlaylist(context: Context, song: Song) {
    LoadPlaylistAsync(WeakReference(context), true) {
        val playlistDatabaseHelper =
            PlaylistDatabaseHelper(context)
        val playlistList = playlistDatabaseHelper.getAllPlaylists()

        val playlistNames = arrayOfNulls<String>(playlistList.size)
        val playlistIds = arrayOfNulls<String>(playlistList.size)

        for (i in playlistList.indices) {
            playlistNames[i] = playlistList[i].playlistName
            playlistIds[i] = playlistList[i].playlistId
        }

        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle(context.getString(R.string.pickPlaylistMsg))
        alertDialogBuilder.setItems(playlistNames) { _, i ->
            playlistIds[i]?.let { playlistId ->
                val addToPlaylistAsync = AddToPlaylistAsync(WeakReference(context), playlistId, song)
                addToPlaylistAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }

        }
        alertDialogBuilder.show()
    }
}

/**
 * Create a new playlist, will ask for name with alertDialog
 */
internal fun createPlaylist(context:Context){
    val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    val playlistNameInputLayout = layoutInflater.inflate(R.layout.playlistname_input_layout, null)

    AlertDialog.Builder(context).apply {
        setTitle(context.getString(R.string.createPlaylist))
        setPositiveButton(context.getString(R.string.ok)) { _, _ ->
            val playlistNameEditText = playlistNameInputLayout.findViewById<EditText>(R.id.playlistNameInput)
            val playlistName = playlistNameEditText.text.toString()

            val createPlaylistAsync = CreatePlaylistAsync(WeakReference(context), playlistName)
            createPlaylistAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
        setView(playlistNameInputLayout)
        setCancelable(true)
    }.create().run {
        show()
    }
}

/**
 * Delete playlist with given playlistId
 */
internal fun deletePlaylist(context: Context, playlistId:String){
    val deletePlaylistAsync = DeletePlaylistAsync(WeakReference(context), playlistId)
    deletePlaylistAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

/**
 * Delete one song from playlist, index required because double songs can exist in one playlist
 */
internal fun deletePlaylistSong(context: Context, song: Song, playlistId: String, index:Int, playlistMax:Int){
    //for playlist sorting features
    val playlistSortedByNew = true

    if(playlistSortedByNew){
        //required because api sorts by oldest songs added
        val songDeleteIndex = playlistMax - index

        val deletePlaylistTrackAsync = DeletePlaylistTrackAsync(WeakReference(context), song , playlistId, songDeleteIndex)
        deletePlaylistTrackAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}