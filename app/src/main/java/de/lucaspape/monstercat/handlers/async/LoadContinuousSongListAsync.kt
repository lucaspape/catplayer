package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.addContinuous
import de.lucaspape.monstercat.util.Settings
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

            for(songId in songIdList){

                val song = songDatabaseHelper.getSong(songId)

                if (song != null) {
                    //check if song is already downloaded
                    song.downloadLocation =
                        context.getExternalFilesDir(null).toString() + "/" + song.artist + song.title + song.version + "." + downloadType

                    song.streamLocation = "https://connect.monstercat.com/v2/release/" +  song.albumId + "/track-stream/" + song.songId

                    addContinuous(song)
                }

            }
        }

        return null

    }

}