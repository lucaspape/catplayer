package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.getSid
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.addContinuous
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.Settings
import de.lucaspape.monstercat.util.parseObjectToStreamHash
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference

class LoadContinuousSongListAsync(
    private val songIdList: ArrayList<String>,
    private val contextReference: WeakReference<Context>
) : AsyncTask<Void, Void, String>() {
    override fun doInBackground(vararg params: Void?): String? {
        contextReference.get()?.let { context ->
            val settings = Settings(context)
            val downloadType = settings.getSetting("downloadType")

            val songDatabaseHelper = SongDatabaseHelper(context)

            val streamHashQueue = Volley.newRequestQueue(context)

            val syncObject = Object()

            streamHashQueue.addRequestFinishedListener<Any> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            for (songId in songIdList) {
                val song = songDatabaseHelper.getSong(songId)
                if (song != null) {
                    //check if song is already downloaded
                    val songDownloadLocation =
                        context.getExternalFilesDir(null).toString() + "/" + song.artist + song.title + song.version + "." + downloadType

                    if (File(songDownloadLocation).exists()) {
                        song.downloadLocation = songDownloadLocation
                        addContinuous(song)
                    } else {
                        //get stream hash
                        val streamHashUrl =
                            context.getString(R.string.loadSongsUrl) + "?albumId=" + song.albumId

                        val hashRequest = AuthorizedRequest(
                            Request.Method.GET, streamHashUrl, getSid(),
                            Response.Listener { response ->
                                val jsonObject = JSONObject(response)

                                val streamHash = parseObjectToStreamHash(jsonObject, song)

                                if (streamHash != null) {
                                    song.streamLocation =
                                        context.getString(R.string.songStreamUrl) + streamHash
                                    addContinuous(song)
                                }
                            },
                            Response.ErrorListener { })

                        streamHashQueue.add(hashRequest)

                        synchronized(syncObject) {
                            syncObject.wait()
                        }
                    }
                }

            }


        }

        return null

    }

}