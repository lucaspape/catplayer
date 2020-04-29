package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.newLoadRelatedTracksRequest
import de.lucaspape.monstercat.util.newAuthorizedRequestQueue
import org.json.JSONException
import java.lang.ref.WeakReference

class LoadRelatedTracksAsync(
    private val contextReference: WeakReference<Context>,
    private val trackIdArray: ArrayList<String>,
    private val skipMC: Boolean,
    private val finishedCallback: (trackIdArray: ArrayList<String>, relatedIdArray: ArrayList<String>) -> Unit,
    private val errorCallback: (trackIdArray: ArrayList<String>) -> Unit
) : AsyncTask<Void, Void, ArrayList<String>?>() {

    override fun onPostExecute(result: ArrayList<String>?) {
        if (result != null) {
            finishedCallback(trackIdArray, result)
        } else {
            errorCallback(trackIdArray)
        }
    }

    override fun doInBackground(vararg params: Void?): ArrayList<String>? {
        contextReference.get()?.let { context ->
            val syncObject = Object()

            var result: ArrayList<String>? = ArrayList()

            val volleyQueue = newAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            volleyQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            volleyQueue.add(newLoadRelatedTracksRequest(context, trackIdArray, skipMC, {
                try {
                    val relatedJsonArray = it.getJSONArray("results")

                    for (i in (0 until relatedJsonArray.length())) {
                        val trackObject = relatedJsonArray.getJSONObject(i)
                        result?.add(trackObject.getString("id"))
                    }
                } catch (e: JSONException) {
                    result = null
                }
            }, {result = null}))

            synchronized(syncObject) {
                syncObject.wait()

                return result
            }
        }

        return null
    }

}