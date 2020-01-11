package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.util.displayInfo
import de.lucaspape.monstercat.util.sid
import org.json.JSONObject
import java.lang.ref.WeakReference

class AddToPlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String,
    private val song: Song
) : AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg params: Void?): String? {
        contextReference.get()?.let { context ->
            val addToPlaylistUrl = context.getString(R.string.addToPlaylistUrl)

            val addToPlaylistQueue = Volley.newRequestQueue(context)

            val songJsonObject = JSONObject()
            songJsonObject.put("releaseId", song.albumId)
            songJsonObject.put("trackId", song.songId)

            val putJsonObject = JSONObject()
            putJsonObject.put("playlistId", playlistId)
            putJsonObject.put("newSong", songJsonObject)
            putJsonObject.put("sid", sid)

            val addToPlaylistRequest =
                JsonObjectRequest(Request.Method.POST, addToPlaylistUrl, putJsonObject,
                    Response.Listener { response ->
                        //TODO reload playlist from response
                        displayInfo(
                            context,
                            context.getString(R.string.songAddedToPlaylistMsg, song.shownTitle)
                        )
                    },
                    Response.ErrorListener { error ->
                        displayInfo(context, context.getString(R.string.errorUpdatePlaylist))
                    }
                )

            addToPlaylistQueue.add(addToPlaylistRequest)
        }

        return null
    }

}