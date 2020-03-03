package de.lucaspape.monstercat.handlers

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.EditText
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.background.BackgroundService.Companion.loadContinuousSongListAsyncTask
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.handlers.async.*
import de.lucaspape.monstercat.music.playNext
import de.lucaspape.monstercat.music.songQueue
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.Settings
import de.lucaspape.monstercat.util.displayInfo
import de.lucaspape.monstercat.util.parseSongToDB
import de.lucaspape.monstercat.util.sid
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference

/**
 * Play a song from ID
 */
internal fun playSongFromId(songId: String, playNow: Boolean) {
    if (playNow) {
        songQueue.add(songId)
        playNext()
    } else {
        songQueue.add(0, songId)
    }
}

/**
 * Play a song from ID with next songs (-> songs which are listed under the current song in listView)
 */
internal fun playSongFromId(
    context: Context,
    songId: String,
    playNow: Boolean,
    nextSongIds: ArrayList<String>
) {
    playSongFromId(songId, playNow)

    val continuousList = ArrayList<String>()

    for (nextSongId in (nextSongIds)) {
        continuousList.add(nextSongId)
    }

    loadContinuousSongListAsyncTask?.cancel(true)

    loadContinuousSongListAsyncTask = BackgroundAsync({
        val songDatabaseHelper = SongDatabaseHelper(context)

        val skipMonstercatSongs = Settings(context).getSetting("skipMonstercatSongs")

        for (cSongId in continuousList) {

            val song = songDatabaseHelper.getSong(context, cSongId)

            if (song != null) {
                if(song.artist.contains("monstercat", true)){
                    if(skipMonstercatSongs != "true"){
                        songQueue.add(song.songId)
                    }
                }else{
                    songQueue.add(song.songId)
                }
            }
        }
    }, {})

    loadContinuousSongListAsyncTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

/**
 * Play an entire album after the current song
 */
internal fun playAlbumNext(context: Context, mcID: String) {
    val requestUrl =
        context.getString(R.string.loadAlbumSongsUrl) + "/" + mcID

    val albumRequestQueue = Volley.newRequestQueue(context)

    val albumRequest = AuthorizedRequest(Request.Method.GET, requestUrl,
        sid,
        Response.Listener { response ->
            val jsonObject = JSONObject(response)
            val jsonArray = jsonObject.getJSONArray("tracks")

            val idArray = ArrayList<String>()

            for (i in (0 until jsonArray.length())) {
                parseSongToDB(jsonArray.getJSONObject(i), context)?.let { id ->
                    idArray.add(id)
                }
            }

            songQueue.add(idArray[0])

            loadContinuousSongListAsyncTask?.cancel(true)

            loadContinuousSongListAsyncTask = BackgroundAsync({
                for (i in (1 until idArray.size)) {
                    songQueue.add(idArray[i])
                }
            }, {})

            loadContinuousSongListAsyncTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        },
        Response.ErrorListener {
            displayInfo(context, context.getString(R.string.errorRetrieveAlbumData))
        })

    albumRequestQueue.add(albumRequest)
}

internal fun playPlaylistNext(context: Context, playlistId: String) {
    LoadPlaylistTracksAsync(WeakReference(context), true, playlistId, {}) {
        val playlistItemDatabaseHelper =
            PlaylistItemDatabaseHelper(context, playlistId)
        val playlistItemList = playlistItemDatabaseHelper.getAllData().reversed()

        val songDatabaseHelper = SongDatabaseHelper(context)

        songDatabaseHelper.getSong(
            context,
            playlistItemList[0].songId
        )?.songId?.let { songId ->
            songQueue.add(songId)
        }

        loadContinuousSongListAsyncTask?.cancel(true)

        loadContinuousSongListAsyncTask = BackgroundAsync({
            for (i in (1 until playlistItemList.size)) {
                songDatabaseHelper.getSong(
                    context,
                    playlistItemList[i].songId
                )?.songId?.let { songId ->
                    songQueue.add(songId)
                }
            }
        }, {})

        loadContinuousSongListAsyncTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

/**
 * Download an entire playlist
 */
internal fun downloadPlaylist(context: Context, playlistId: String, downloadFinished: () -> Unit) {
    LoadPlaylistTracksAsync(WeakReference(context), true, playlistId, {}) {
        val playlistItemDatabaseHelper =
            PlaylistItemDatabaseHelper(context, playlistId)
        val playlistItemList = playlistItemDatabaseHelper.getAllData()

        for (playlistItem in playlistItemList) {
            val songDatabaseHelper = SongDatabaseHelper(context)
            val song = songDatabaseHelper.getSong(context, playlistItem.songId)

            song?.songId?.let { addDownloadSong(context, it, downloadFinished) }
        }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

/**
 * Deletes downloaded playlist tracks
 */
internal fun deleteDownloadedPlaylistTracks(
    context: Context,
    playlistId: String,
    deleteFinished: () -> Unit
) {
    val alertDialogBuilder = AlertDialog.Builder(context)
    alertDialogBuilder.setTitle(context.getString(R.string.deletePlaylistDownloadedTracksMsg))
    alertDialogBuilder.setPositiveButton(context.getString(R.string.yes)) { _, _ ->
        val playlistItemDatabaseHelper = PlaylistItemDatabaseHelper(context, playlistId)
        val playlistItemList = playlistItemDatabaseHelper.getAllData()

        val songDatabaseHelper = SongDatabaseHelper(context)

        for (playlistItem in playlistItemList) {
            val song = songDatabaseHelper.getSong(context, playlistItem.songId)

            song?.let {
                File(song.downloadLocation).delete()
            }
        }

        deleteFinished()
    }

    alertDialogBuilder.setNegativeButton(context.getString(R.string.no)) { _, _ -> }

    val dialog = alertDialogBuilder.create()
    dialog.show()

    val typedValue = TypedValue()
    context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

    val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
    positiveButton.setTextColor(typedValue.data)

    val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
    negativeButton.setTextColor(typedValue.data)
}

/**
 * Download an entire album
 */
internal fun downloadAlbum(context: Context, mcID: String) {
    val requestUrl =
        context.getString(R.string.loadAlbumSongsUrl) + "/" + mcID

    val albumRequestQueue = Volley.newRequestQueue(context)

    val albumRequest = AuthorizedRequest(Request.Method.GET, requestUrl,
        sid,
        Response.Listener { response ->
            val jsonObject = JSONObject(response)
            val jsonArray = jsonObject.getJSONArray("tracks")

            val idArray = ArrayList<String>()

            for (i in (0 until jsonArray.length())) {
                parseSongToDB(jsonArray.getJSONObject(i), context)?.let { id ->
                    idArray.add(id)
                }
            }

            val databaseHelper = SongDatabaseHelper(context)

            for (id in idArray) {
                val song = databaseHelper.getSong(context, id)
                song?.songId?.let { addDownloadSong(context, it) {} }
            }
        },
        Response.ErrorListener {
            displayInfo(context, context.getString(R.string.errorRetrieveAlbumData))
        })

    albumRequestQueue.add(albumRequest)
}

/**
 * Add single song to playlist, will ask for playlist with alertDialog TODO check if logged in
 */
internal fun addSongToPlaylist(context: Context, song: Song) {
    LoadPlaylistAsync(WeakReference(context), true, {}) {
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
                val addToPlaylistAsync =
                    AddToPlaylistAsync(WeakReference(context), playlistId, song)
                addToPlaylistAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }

        }
        alertDialogBuilder.show()
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

/**
 * Create a new playlist, will ask for name with alertDialog TODO check if logged in
 */
internal fun createPlaylist(context: Context) {
    val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    val playlistNameInputLayout = layoutInflater.inflate(R.layout.playlistname_input_layout, null)

    AlertDialog.Builder(context).apply {
        setTitle(context.getString(R.string.createPlaylist))
        setPositiveButton(context.getString(R.string.ok)) { _, _ ->
            val playlistNameEditText =
                playlistNameInputLayout.findViewById<EditText>(R.id.playlistNameInput)
            val playlistName = playlistNameEditText.text.toString()

            val createPlaylistAsync = CreatePlaylistAsync(WeakReference(context), playlistName)
            createPlaylistAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
        setView(playlistNameInputLayout)
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

/**
 * Delete playlist with given playlistId
 */
internal fun deletePlaylist(context: Context, playlistId: String) {
    val alertDialogBuilder = AlertDialog.Builder(context)
    alertDialogBuilder.setTitle(context.getString(R.string.deletePlaylistMsg))
    alertDialogBuilder.setPositiveButton(context.getString(R.string.yes)) { _, _ ->
        val deletePlaylistAsync = DeletePlaylistAsync(WeakReference(context), playlistId)
        deletePlaylistAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
    alertDialogBuilder.setNegativeButton(context.getString(R.string.no)) { _, _ -> }

    val dialog = alertDialogBuilder.create()
    dialog.show()

    val typedValue = TypedValue()
    context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

    val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
    positiveButton.setTextColor(typedValue.data)

    val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
    negativeButton.setTextColor(typedValue.data)
}

/**
 * Delete one song from playlist, index required because double songs can exist in one playlist
 */
internal fun deletePlaylistSong(
    context: Context,
    song: Song,
    playlistId: String,
    index: Int,
    playlistMax: Int
) {
    //for playlist sorting features
    val playlistSortedByNew = true

    if (playlistSortedByNew) {
        //required because api sorts by oldest songs added
        val songDeleteIndex = playlistMax - index

        val deletePlaylistTrackAsync =
            DeletePlaylistTrackAsync(WeakReference(context), song, playlistId, songDeleteIndex)
        deletePlaylistTrackAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}

internal fun openAlbum(context: Context, albumMcId: String, share: Boolean) {
    val volleyRequestQueue = Volley.newRequestQueue(context)

    val albumLinksRequest = AuthorizedRequest(Request.Method.GET,
        context.getString(R.string.loadAlbumSongsUrl) + "/$albumMcId",
        sid,
        Response.Listener { response ->
            val responseJsonObject = JSONObject(response)
            val releaseObject = responseJsonObject.getJSONObject("release")
            val linksArray = releaseObject.getJSONArray("links")

            val titles = arrayOfNulls<String>(linksArray.length() + 1)
            val urls = arrayOfNulls<String>(linksArray.length() + 1)

            titles[0] = "monstercat.com"
            urls[0] = context.getString(R.string.shareReleaseUrl) + "/$albumMcId"

            for (i in (0 until linksArray.length())) {
                val linkObject = linksArray.getJSONObject(i)

                titles[i + 1] = linkObject.getString("platform")
                urls[i + 1] = linkObject.getString("original")
            }

            val alertDialogBuilder = AlertDialog.Builder(context)

            if (share) {
                alertDialogBuilder.setTitle(context.getString(R.string.pickLinkToShare))
            } else {
                alertDialogBuilder.setTitle(context.getString(R.string.pickApp))
            }

            alertDialogBuilder.setItems(titles) { _, i ->
                urls[i]?.let { url ->
                    if (share) {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, url)
                            type = "text/plain"
                        }

                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    } else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }

            }
            alertDialogBuilder.show()
        },
        Response.ErrorListener {
            displayInfo(context, context.getString(R.string.errorRetrieveAlbumData))
        })

    volleyRequestQueue.add(albumLinksRequest)
}