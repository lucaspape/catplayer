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
import de.lucaspape.monstercat.util.getSid
import org.json.JSONObject
import java.lang.ref.WeakReference

class DeletePlaylistTrackAsync(private val contextReference:WeakReference<Context>, private val song: Song, private val playlistId:String, private val songDeleteIndex:Int) : AsyncTask<Void, Void, String>(){
    override fun doInBackground(vararg params: Void?): String? {
        contextReference.get()?.let { context ->
            val deleteSongObject = JSONObject()
            deleteSongObject.put("songDeleteIndex", songDeleteIndex)
            deleteSongObject.put("releaseId", song.albumId)
            deleteSongObject.put("trackId", song.songId)

            val deleteObject = JSONObject()
            deleteObject.put("songDelete", deleteSongObject)
            deleteObject.put("sid", getSid())
            deleteObject.put("playlistId", playlistId)

            val deleteSongVolleyQueue = Volley.newRequestQueue(context)

            val deleteSongRequest = JsonObjectRequest(
                Request.Method.POST, context.getString(R.string.removeFromPlaylistUrl), deleteObject,
                Response.Listener { response ->
                    displayInfo(context, context.getString(R.string.removedSongFromPlaylistMsg))
                },
                Response.ErrorListener { error ->
                    displayInfo(context, context.getString(R.string.errorRemovingSongFromPlaylist))
                })

            deleteSongVolleyQueue.add(deleteSongRequest)
        }

        return null
    }

}