package de.lucaspape.monstercat.handlers

import android.app.AlertDialog
import android.content.Context
import android.os.AsyncTask
import android.widget.ListView
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.loadContinuousSongListAsyncTask
import de.lucaspape.monstercat.database.*
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.handlers.async.LoadContinuousSongListAsync
import de.lucaspape.monstercat.music.addSong
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference

internal fun playSongFromId(context: Context, songId: String, playNow: Boolean) {
    val settings = Settings(context)
    val downloadType = settings.getSetting("downloadType")

    val songDatabaseHelper = SongDatabaseHelper(context)
    val song = songDatabaseHelper.getSong(songId)

    if (song != null) {
        song.downloadLocation =
            context.getExternalFilesDir(null).toString() + "/" + song.artist + song.title + song.version + "." + downloadType

        song.streamLocation =
            context.getString(R.string.trackContentUrl) + song.albumId + "/track-stream/" + song.songId

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
 * With next songs
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

            val settings = Settings(context)
            val downloadType = settings.getSetting("downloadType")

            idArray.reverse()

            for (id in idArray) {
                val song = databaseHelper.getSong(id)

                song.downloadLocation =
                    context.getExternalFilesDir(null).toString() + "/" + song.artist + song.title + song.version + "." + downloadType

                song.streamLocation =
                    context.getString(R.string.trackContentUrl) + song.albumId + "/track-stream/" + song.songId

                addSong(song)
            }
        },
        Response.ErrorListener { error ->
            displayInfo(context, context.getString(R.string.errorRetrieveAlbumData))
        })

    albumRequestQueue.add(albumRequest)
}

internal fun downloadPlaylist(context: Context, playlistId: String) {
    val playlistItemDatabaseHelper =
        PlaylistItemDatabaseHelper(context, playlistId)
    val playlistItemList = playlistItemDatabaseHelper.getAllData()

    for (playlistItem in playlistItemList) {
        val songDatabaseHelper = SongDatabaseHelper(context)
        val song = songDatabaseHelper.getSong(playlistItem.songId)

        downloadSong(context, song)
    }
}

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
        displayInfo(context, context.getString(R.string.downloadNotAvailableMsg, song.title + " " + song.version))
    }
}

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
                val song = databaseHelper.getSong(id)
                downloadSong(context, song)
            }
        },
        Response.ErrorListener { error ->
            displayInfo(context, context.getString(R.string.errorRetrieveAlbumData))
        })

    albumRequestQueue.add(albumRequest)
}

internal fun addSongToPlaylist(context: Context, song: Song) {
    val playlistDatabaseHelper =
        PlaylistDatabaseHelper(context)
    val playlistList = playlistDatabaseHelper.getAllPlaylists()

    val playlistNames = arrayOfNulls<String>(playlistList.size)
    val playlistIds = arrayOfNulls<String>(playlistList.size)

    for (i in playlistList.indices) {
        playlistNames[i] = playlistList[i].playlistName
        playlistIds[i] = playlistList[i].playlistId
    }

    val sSid = getSid()

    if (sSid != null) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle(context.getString(R.string.pickPlaylistMsg))
        alertDialogBuilder.setItems(playlistNames) { _, i ->
            val playlistPatchUrl = context.getString(R.string.playlistUrl) + playlistIds[i]

            playlistIds[i]?.let { playlistId ->
                val trackCountRequestQueue = Volley.newRequestQueue(context)

                var trackCount = 0

                val trackCountRequest = AuthorizedRequest(Request.Method.GET,
                    context.getString(R.string.playlistsUrl),
                    getSid(),
                    Response.Listener { response ->
                        val jsonObject = JSONObject(response)
                        val jsonArray = jsonObject.getJSONArray("results")

                        for (u in (0 until jsonArray.length())) {
                            if (jsonArray.getJSONObject(u).getString("_id") == playlistId) {
                                val tracks = jsonArray.getJSONObject(u).getJSONArray("tracks")
                                trackCount = tracks.length()
                                break
                            }
                        }
                    },
                    Response.ErrorListener {
                        displayInfo(context, context.getString(R.string.errorRetrievePlaylist))
                    })


                trackCountRequestQueue.addRequestFinishedListener<Any> {
                    val tempList = arrayOfNulls<JSONObject>(trackCount)

                    val playlistTrackRequestQueue = Volley.newRequestQueue(context)

                    playlistTrackRequestQueue.addRequestFinishedListener<Any> {
                        val patchParams = JSONObject()

                        val patchedArray = parsePatchedPlaylist(JSONArray(tempList), song)
                        patchParams.put("tracks", JSONArray(patchedArray))

                        val patchRequest =
                            object : JsonObjectRequest(
                                Method.PATCH,
                                playlistPatchUrl,
                                patchParams,
                                Response.Listener {
                                    //TODO reload playlist
                                },
                                Response.ErrorListener {
                                }) {
                                @Throws(AuthFailureError::class)
                                override fun getHeaders(): Map<String, String> {
                                    val params = HashMap<String, String>()
                                    if (loggedIn) {
                                        params["Cookie"] = "connect.sid=$sSid"
                                    }
                                    return params
                                }
                            }

                        val addToPlaylistQueue = Volley.newRequestQueue(context)
                        addToPlaylistQueue.addRequestFinishedListener<Any> {
                            displayInfo(
                                context, context.getString(
                                    R.string.songAddedToPlaylistMsg,
                                    song.title + " " + song.version
                                )
                            )
                        }

                        addToPlaylistQueue.add(patchRequest)

                    }


                    val playlistTrackUrl =
                        context.getString(R.string.playlistTrackUrl) + playlistId

                    val playlistTrackRequest = AuthorizedRequest(
                        Request.Method.GET, playlistTrackUrl, getSid(),
                        Response.Listener { response ->
                            val jsonObject = JSONObject(response)
                            val jsonArray = jsonObject.getJSONArray("tracks")

                            for (k in (0 until jsonArray.length())) {
                                tempList[k] = jsonArray.getJSONObject(k)
                            }
                        },
                        Response.ErrorListener {
                            displayInfo(context, context.getString(R.string.errorUpdatePlaylist))
                        })

                    playlistTrackRequestQueue.add(playlistTrackRequest)

                }

                trackCountRequestQueue.add(trackCountRequest)
            }

        }
        alertDialogBuilder.show()

    } else {
        displayInfo(context, context.getString(R.string.errorNotLoggedIn))
    }
}