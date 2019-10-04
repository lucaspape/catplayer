package de.lucaspape.monstercat.handlers

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.activities.MainActivity
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.loggedIn
import de.lucaspape.monstercat.auth.sid
import de.lucaspape.monstercat.database.*
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.json.JSONParser
import de.lucaspape.monstercat.music.addSong
import de.lucaspape.monstercat.settings.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

fun playSongFromId(context: Context, songId:String, playNow: Boolean) {
    val settings = Settings(context)
    val downloadType = settings.getSetting("downloadType")

    val songDatabaseHelper = SongDatabaseHelper(context)
    val song = songDatabaseHelper.getSong(songId)

    if(song != null){
        //check if song is already downloaded
        val songDownloadLocation =
            context.filesDir.toString() + "/" + song.artist + song.title + song.version + "." + downloadType

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

            val hashRequest = object : StringRequest(
                Method.GET, streamHashUrl,
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
                    } else {
                        //could not find song
                    }
                },
                Response.ErrorListener { }) {
                //add authentication
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    if (loggedIn) {
                        params["Cookie"] = "connect.sid=$sid"
                    }
                    return params
                }
            }

            streamHashQueue.add(hashRequest)
        }
    }
}

fun downloadPlaylist(context: Context, playlistId: String) {
    val playlistDataDatabaseHelper = PlaylistDataDatabaseHelper(context, playlistId)
    val playlistDataList = playlistDataDatabaseHelper.getAllData()

    for(playlistData in playlistDataList){
        val songDatabaseHelper = SongDatabaseHelper(context)
        val song = songDatabaseHelper.getSong(playlistData.songId)

        downloadSong(context, song)
    }
}

fun downloadSong(context: Context, song:Song) {
    val settings = Settings(context)

    val downloadType = settings.getSetting("downloadType")
    val downloadQuality = settings.getSetting("downloadQuality")

    val downloadUrl =
        context.getString(R.string.songDownloadUrl) + song.albumId + "/download?method=download&type=" + downloadType + "_" + downloadQuality + "&track=" + song.songId

    val downloadLocation = context.filesDir.toString() + "/" + song.artist + song.title + song.version + "." + downloadType

    if (!File(downloadLocation).exists()) {
        if (sid != "") {
            addDownloadSong(downloadUrl, downloadLocation, song.title + song.version)
        }else{
            Toast.makeText(context, context.getString(R.string.userNotSignedInMsg), Toast.LENGTH_SHORT)
                .show()
        }
    }else{
        Toast.makeText(
            context,
            context.getString(R.string.alreadyDownloadedMsg, song.title + song.version),
            Toast.LENGTH_SHORT
        )
            .show()
    }

}

fun addSongToPlaylist(context: Context, song:Song) {
    val playlistDatabaseHelper = PlaylistDatabaseHelper(context)
    val playlistList = playlistDatabaseHelper.getAllPlaylists()

    val playlistNames = arrayOfNulls<String>(playlistList.size)
    val playlistIds = arrayOfNulls<String>(playlistList.size)
    val trackList = arrayOfNulls<List<PlaylistData>>(playlistList.size)

    for(i in playlistList.indices){
        val playlistDataDatabaseHelper = PlaylistDataDatabaseHelper(context, playlistList[i].playlistId)

        playlistNames[i] = playlistList[i].playlistName
        playlistIds[i] = playlistList[i].playlistId
        trackList[i] = playlistDataDatabaseHelper.getAllData()
    }

    val alertDialogBuilder = AlertDialog.Builder(context)
    alertDialogBuilder.setTitle(context.getString(R.string.pickPlaylistMsg))
    alertDialogBuilder.setItems(playlistNames) { _, i ->
        val playlistPatchUrl = context.getString(R.string.playlistUrl) + playlistIds[i]
        val jsonParser = JSONParser()

        if(trackList[i] != null){
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
                        println("ok")
                    },
                    Response.ErrorListener {
                        println("error")
                    }) {
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        val params = HashMap<String, String>()
                        if (loggedIn) {
                            params["Cookie"] = "connect.sid=$sid"
                        }
                        return params
                    }
                }

            val addToPlaylistQueue = Volley.newRequestQueue(context)
            addToPlaylistQueue.addRequestFinishedListener<Any> {
                //TODO add msg
                println("done")
            }

            addToPlaylistQueue.add(patchRequest)

        }
    }
    alertDialogBuilder.show()
}