package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.newCustomApiFeatureRequest
import de.lucaspape.monstercat.util.getAuthorizedRequestQueue
import de.lucaspape.util.Settings
import org.json.JSONException
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference

class CheckCustomApiFeaturesAsync(
    private val contextReference: WeakReference<Context>,
    private val finishedCallback: () -> Unit,
    private val errorCallback: () -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback()
        } else {
            errorCallback()
        }
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        contextReference.get()?.let { context ->
            val newVolleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            var success = true
            val syncObject = Object()

            val settings = Settings.getSettings(context)

            settings.setBoolean(context.getString(R.string.customApiSupportsV1Setting), false)
            settings.setBoolean(context.getString(R.string.customApiSupportsV2Setting), false)

            newVolleyQueue.add(newCustomApiFeatureRequest(context, {
                try {
                    val apiVersionsArray = it.getJSONArray("api_versions")

                    for (i in (0 until apiVersionsArray.length())) {
                        if (apiVersionsArray.getString(i) == "v1") {
                            settings.setBoolean(
                                context.getString(R.string.customApiSupportsV1Setting),
                                true
                            )
                        } else if (apiVersionsArray.getString(i) == "v2") {
                            settings.setBoolean(
                                context.getString(R.string.customApiSupportsV2Setting),
                                true
                            )
                        }
                    }

                    settings.setString(context.getString(R.string.customDownloadUrlSetting), it.getString("download_base_url"))
                    settings.setString(context.getString(R.string.customStreamUrlSetting), it.getString("stream_base_url"))

                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                } catch (e: JSONException) {
                    success = false

                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }
            }, {
                success = false

                synchronized(syncObject) {
                    syncObject.notify()
                }
            }))

            synchronized(syncObject) {
                syncObject.wait()

                return success
            }
        }

        return false
    }

}