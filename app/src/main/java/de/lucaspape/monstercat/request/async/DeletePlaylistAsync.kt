package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.displayInfo
import de.lucaspape.monstercat.util.sid
import java.lang.ref.WeakReference

class DeletePlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String
) : AsyncTask<Void, Void, String>() {
    override fun doInBackground(vararg params: Void?): String? {
        contextReference.get()?.let { context ->
            val deletePlaylistVolleyQueue = Volley.newRequestQueue(context)

            val deletePlaylistUrl = context.getString(R.string.playlistUrl) + playlistId

            val deletePlaylistRequest = AuthorizedRequest(
                Request.Method.DELETE, deletePlaylistUrl, sid,
                Response.Listener {
                    displayInfo(context, context.getString(R.string.playlistDeletedMsg))
                },
                Response.ErrorListener {
                    displayInfo(context, context.getString(R.string.errorDeletingPlaylist))
                })

            deletePlaylistVolleyQueue.add(deletePlaylistRequest)
        }

        return null
    }

}