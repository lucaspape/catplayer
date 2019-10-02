package de.lucaspape.monstercat.handlers

import android.app.AlertDialog
import android.content.Context
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.loggedIn
import de.lucaspape.monstercat.auth.sid
import de.lucaspape.monstercat.database.SongDatabaseHelper
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

//TODO implement
fun downloadPlaylist(context: Context, playlistId: String) {

}

//TODO implement
fun downloadSong(context: Context, songId:String) {

}

@Deprecated("currently not working")
fun addSongToPlaylist(song: HashMap<String, Any?>, context: Context) {
    var playlistNames = arrayOfNulls<String>(0)
    var playlistIds = arrayOfNulls<String>(0)
    var tracksArrays = arrayOfNulls<JSONArray>(0)

    val queue = Volley.newRequestQueue(context)

    //get all playlists
    val playlistUrl = context.getString(R.string.playlistsUrl)

    val playlistRequest = object : StringRequest(playlistUrl,
        Response.Listener { response ->
            val jsonObject = JSONObject(response)
            val jsonArray = jsonObject.getJSONArray("results")

            playlistNames = arrayOfNulls(jsonArray.length())
            playlistIds = arrayOfNulls(jsonArray.length())
            tracksArrays = arrayOfNulls(jsonArray.length())

            for (i in (0 until jsonArray.length())) {
                val playlistObject = jsonArray.getJSONObject(i)

                val playlistName = playlistObject.getString("name")
                val playlistId = playlistObject.getString("_id")
                val trackArray = playlistObject.getJSONArray("tracks")

                playlistNames[i] = playlistName
                playlistIds[i] = playlistId
                tracksArrays[i] = trackArray
            }
        },

        Response.ErrorListener { _ ->

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

    queue.addRequestFinishedListener<Any> {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle(context.getString(R.string.pickPlaylistMsg))
        alertDialogBuilder.setItems(playlistNames) { _, i ->
            val playlistPatchUrl = context.getString(R.string.playlistUrl) + playlistIds[i]
            val patchParams = JSONObject()

            val trackArray = tracksArrays[i]

            val jsonParser = JSONParser()
            val patchedArray = jsonParser.parsePatchedPlaylist(trackArray!!, song)

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
                            params["Cookie"] = "connect.sid=$sid"
                        }
                        return params
                    }
                }

            val addToPlaylistQueue = Volley.newRequestQueue(context)
            addToPlaylistQueue.addRequestFinishedListener<Any> {
                //TODO add msg
            }

            addToPlaylistQueue.add(patchRequest)
        }
        alertDialogBuilder.show()
    }

    queue.add(playlistRequest)
}