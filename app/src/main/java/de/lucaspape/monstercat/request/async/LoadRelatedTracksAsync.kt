package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

class LoadRelatedTracksAsync(
    private val contextReference: WeakReference<Context>,
    private val trackIdArray: ArrayList<String>,
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

            val volleyQueue = Volley.newRequestQueue(context)

            volleyQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            val jsonObject = JSONObject()
            val trackJsonArray = JSONArray()

            for(i in(trackIdArray.size -10 until trackIdArray.size)){
                val trackObject = JSONObject()
                trackObject.put("id", trackIdArray[i])
                trackJsonArray.put(trackObject)
            }

            jsonObject.put("tracks", trackJsonArray)

            val relatedTracksRequest = JsonObjectRequest(Request.Method.POST,
                context.getString(R.string.customApiBaseUrl) + "related",
                jsonObject,
                Response.Listener {
                    try {
                        val relatedJsonArray = it.getJSONArray("results")

                        for(i in(0 until relatedJsonArray.length())){
                            val trackObject = relatedJsonArray.getJSONObject(i)
                            result?.add(trackObject.getString("id"))
                        }
                    }catch (e: JSONException){
                        result = null
                    }

                },
                Response.ErrorListener {
                    result = null
                })

            relatedTracksRequest.retryPolicy =
                DefaultRetryPolicy(20000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

            volleyQueue.add(relatedTracksRequest)

            synchronized(syncObject) {
                syncObject.wait()

                return result
            }
        }

        return null
    }

}