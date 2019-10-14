package de.lucaspape.monstercat.handlers.util

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.sid
import de.lucaspape.monstercat.database.SongDatabaseHelper
import de.lucaspape.monstercat.json.JSONParser
import de.lucaspape.monstercat.music.addContinuous
import de.lucaspape.monstercat.music.addSong
import de.lucaspape.monstercat.music.playNow
import de.lucaspape.monstercat.request.MonstercatRequest
import de.lucaspape.monstercat.settings.Settings
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference

class LoadContinuousSongListAsync (private val songIdList:ArrayList<String>, private val contextReference: WeakReference<Context>) : AsyncTask<Void, Void, String>(){
    override fun doInBackground(vararg params: Void?): String? {
        val settings = Settings(contextReference.get()!!)
        val downloadType = settings.getSetting("downloadType")

        val songDatabaseHelper = SongDatabaseHelper(contextReference.get()!!)

        for(songId in songIdList){
            val song = songDatabaseHelper.getSong(songId)
            val syncObject = Object()

            if(song != null){
                //check if song is already downloaded
                val songDownloadLocation =
                    contextReference.get()!!.filesDir.toString() + "/" + song.artist + song.title + song.version + "." + downloadType

                if (File(songDownloadLocation).exists()) {
                    song.downloadLocation = songDownloadLocation
                    addContinuous(song)
                } else {
                    val streamHashQueue = Volley.newRequestQueue(contextReference.get()!!)

                    //get stream hash
                    val streamHashUrl =
                        contextReference.get()!!.getString(R.string.loadSongsUrl) + "?albumId=" + song.albumId

                    val hashRequest = MonstercatRequest(
                        Request.Method.GET, streamHashUrl, sid,
                        Response.Listener { response ->
                            val jsonObject = JSONObject(response)

                            val jsonParser = JSONParser()
                            val streamHash = jsonParser.parseObjectToStreamHash(jsonObject, song)

                            if (streamHash != null) {
                                song.streamLocation =
                                    contextReference.get()!!.getString(R.string.songStreamUrl) + streamHash
                                    addContinuous(song)
                            } else {
                                //could not find song
                                //TODO msg
                            }
                        },
                        Response.ErrorListener { })

                    streamHashQueue.addRequestFinishedListener<Any> {
                        synchronized(syncObject){
                            syncObject.notify()
                        }
                    }

                    streamHashQueue.add(hashRequest)

                    synchronized(syncObject){
                        syncObject.wait()
                    }
                }
            }

        }

        return null

    }

}