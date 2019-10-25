package de.lucaspape.monstercat.handlers

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.getSid
import de.lucaspape.monstercat.auth.loggedIn
import de.lucaspape.monstercat.database.*
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.json.JSONParser
import de.lucaspape.monstercat.music.addSong
import de.lucaspape.monstercat.request.MonstercatRequest
import de.lucaspape.monstercat.settings.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal fun playSongFromId(context: Context, songId: String, playNow: Boolean) {
    val settings = Settings(context)
    val downloadType = settings.getSetting("downloadType")

    val songDatabaseHelper = SongDatabaseHelper(context)
    val song = songDatabaseHelper.getSong(songId)

    if (song != null) {
        //check if song is already downloaded
        val songDownloadLocation =
            context.getExternalFilesDir(null).toString() + "/" + song.artist + song.title + song.version + "." + downloadType

        if (File(songDownloadLocation).exists()) {
            song.downloadLocation = songDownloadLocation

            if (playNow) {
                de.lucaspape.monstercat.music.playNow(song)
            } else {
                addSong(song)
            }


        } else {
            val streamHashQueue = Volley.newRequestQueue(context)

            //get stream hash
            val streamHashUrl =
                context.getString(R.string.loadSongsUrl) + "?albumId=" + song.albumId

            val hashRequest = MonstercatRequest(
                Request.Method.GET, streamHashUrl, getSid(),
                Response.Listener { response ->
                    val jsonObject = JSONObject(response)

                    val jsonParser = JSONParser()
                    val streamHash = jsonParser.parseObjectToStreamHash(jsonObject, song)

                    if (streamHash != null) {
                        song.streamLocation =
                            context.getString(R.string.songStreamUrl) + streamHash

                        if (playNow) {
                            de.lucaspape.monstercat.music.playNow(song)
                        } else {
                            addSong(song)
                        }
                    }

                },
                Response.ErrorListener { })

            streamHashQueue.add(hashRequest)
        }
    }
}

internal fun downloadPlaylist(context: Context, playlistId: String) {
    val playlistSongsDatabaseHelper = PlaylistSongsDatabaseHelper(context, playlistId)
    val playlistSongsList = playlistSongsDatabaseHelper.getAllData()

    for (playlistSong in playlistSongsList) {
        val songDatabaseHelper = SongDatabaseHelper(context)
        val song = songDatabaseHelper.getSong(playlistSong.songId)

        downloadSong(context, song)
    }
}

internal fun downloadSong(context: Context, song: Song) {
    val settings = Settings(context)

    val downloadType = settings.getSetting("downloadType")
    val downloadQuality = settings.getSetting("downloadQuality")

    val downloadUrl =
        context.getString(R.string.songDownloadUrl) + song.albumId + "/download?method=download&type=" + downloadType + "_" + downloadQuality + "&track=" + song.songId

    val downloadLocation =
        context.getExternalFilesDir(null).toString() + "/" + song.artist + song.title + song.version + "." + downloadType

    if (!File(downloadLocation).exists()) {
        val sSid = getSid()
        if (sSid != null) {
            addDownloadSong(downloadUrl, downloadLocation, song.title + song.version)
        }

    } else {
        Toast.makeText(
            context,
            context.getString(R.string.alreadyDownloadedMsg, song.title + song.version),
            Toast.LENGTH_SHORT
        )
            .show()
    }

}

internal fun addSongToPlaylist(context: Context, song: Song) {
    val playlistDatabaseHelper = PlaylistDatabaseHelper(context)
    val playlistList = playlistDatabaseHelper.getAllPlaylists()

    val playlistNames = arrayOfNulls<String>(playlistList.size)
    val playlistIds = arrayOfNulls<String>(playlistList.size)
    val trackList = arrayOfNulls<List<PlaylistSongs>>(playlistList.size)

    for (i in playlistList.indices) {
        val playlistSongsDatabaseHelper =
            PlaylistSongsDatabaseHelper(context, playlistList[i].playlistId)

        playlistNames[i] = playlistList[i].playlistName
        playlistIds[i] = playlistList[i].playlistId
        trackList[i] = playlistSongsDatabaseHelper.getAllData()
    }

    val sSid = getSid()

    if (sSid != null) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle(context.getString(R.string.pickPlaylistMsg))
        alertDialogBuilder.setItems(playlistNames) { _, i ->
            val playlistPatchUrl = context.getString(R.string.playlistUrl) + playlistIds[i]
            val jsonParser = JSONParser()


            if (trackList[i] != null) {
                val trackArray = jsonParser.parsePlaylistDataToJSONArray(context, trackList[i]!!)
                val patchParams = JSONObject()

                val patchedArray = jsonParser.parsePatchedPlaylist(JSONArray(trackArray), song)
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
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.songAddedToPlaylistMsg,
                            song.title + " " + song.version
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                addToPlaylistQueue.add(patchRequest)

            }


        }
        alertDialogBuilder.show()

    }
}