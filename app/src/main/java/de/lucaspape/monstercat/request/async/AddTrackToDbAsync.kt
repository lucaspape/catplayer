package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.database.objects.Song
import de.lucaspape.monstercat.util.newAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parseSongToDB
import org.json.JSONObject
import java.lang.ref.WeakReference

class AddTrackToDbAsync(
    private val contextReference: WeakReference<Context>,
    private val trackId: String,
    private val finishedCallback: (trackId: String, song: Song) -> Unit,
    private val errorCallback: (trackId: String) -> Unit
) : AsyncTask<Void, Void, Song?>() {

    override fun onPostExecute(result: Song?) {
        if (result != null) {
            finishedCallback(trackId, result)
        } else {
            errorCallback(trackId)
        }
    }

    override fun doInBackground(vararg params: Void?): Song? {
        contextReference.get()?.let { context ->
            context.getString(R.string.customApiBaseUrl) + "catalog/search?term=$trackId"

            val syncObject = Object()

            val volleyQueue = newAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            volleyQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            val trackRequest = StringRequest(Request.Method.GET,
                context.getString(R.string.customApiBaseUrl) + "catalog/search?term=$trackId",
                Response.Listener {
                    val response = JSONObject(it)
                    val jsonArray = response.getJSONArray("results")

                    for(i in (0 until jsonArray.length())){
                        parseSongToDB(jsonArray.getJSONObject(i), context)
                    }
                },
                Response.ErrorListener { })

            volleyQueue.add(trackRequest)

            synchronized(syncObject) {
                syncObject.wait()

                return SongDatabaseHelper(context).getSong(context, trackId)
            }
        }

        return null
    }

}