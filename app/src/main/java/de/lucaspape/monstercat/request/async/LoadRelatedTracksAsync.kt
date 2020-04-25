package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.newAuthorizedRequestQueue
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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

            val jsonObject = JSONObject()
            val trackJsonArray = JSONArray()
            val excludeJsonArray = JSONArray()

            val startIndex = if (trackIdArray.size >= 10) {
                trackIdArray.size - 10
            } else {
                0
            }

            for (i in (startIndex until trackIdArray.size)) {
                val trackObject = JSONObject()
                trackObject.put("id", trackIdArray[i])
                trackJsonArray.put(trackObject)
            }

            for (trackId in trackIdArray) {
                val trackObject = JSONObject()
                trackObject.put("id", trackId)
                excludeJsonArray.put(trackObject)
            }

            jsonObject.put("tracks", trackJsonArray)
            jsonObject.put("exclude", excludeJsonArray)

            val relatedTracksRequest = JsonObjectRequest(Request.Method.POST,
                context.getString(R.string.customApiBaseUrl) + "related?skipMC=$skipMC",
                jsonObject,
                Response.Listener {
                    try {
                        val relatedJsonArray = it.getJSONArray("results")

                        for (i in (0 until relatedJsonArray.length())) {
                            val trackObject = relatedJsonArray.getJSONObject(i)
                            result?.add(trackObject.getString("id"))
                        }
                    } catch (e: JSONException) {
                        result = null
                    }

                },
                Response.ErrorListener {
                    result = null
                })

            relatedTracksRequest.retryPolicy =
                DefaultRetryPolicy(
                    20000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )

            volleyQueue.add(relatedTracksRequest)

            synchronized(syncObject) {
                syncObject.wait()

                return result
            }
        }

        return null
    }

}