package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.addContinuous
import de.lucaspape.monstercat.util.Settings
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

            for(songId in songIdList){

                val song = songDatabaseHelper.getSong(songId)

                if (song != null) {
                    //check if song is already downloaded
                    val songDownloadLocation =
                        context.getExternalFilesDir(null).toString() + "/" + song.artist + song.title + song.version + "." + downloadType

                    if (File(songDownloadLocation).exists()) {
                        song.downloadLocation = songDownloadLocation
                        addContinuous(song)
                    } else {
                        song.streamLocation = "https://connect.monstercat.com/v2/release/" +  song.albumId + "/track-stream/" + song.songId

                        addContinuous(song)

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