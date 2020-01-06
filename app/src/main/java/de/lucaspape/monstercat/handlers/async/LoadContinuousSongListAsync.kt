package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.addContinuous
import java.lang.ref.WeakReference

class LoadContinuousSongListAsync(
    private val songIdList: ArrayList<String>,
    private val contextReference: WeakReference<Context>
) : AsyncTask<Void, Void, String>() {
    override fun doInBackground(vararg params: Void?): String? {
        contextReference.get()?.let { context ->
            val songDatabaseHelper = SongDatabaseHelper(context)

            for (songId in songIdList) {

                val song = songDatabaseHelper.getSong(context, songId)

                if (song != null) {
                    addContinuous(song.songId)
                }
            }
        }

        return null
    }

}