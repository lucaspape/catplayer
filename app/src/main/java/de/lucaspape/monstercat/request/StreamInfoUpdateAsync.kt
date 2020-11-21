package de.lucaspape.monstercat.request

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.notification.updateNotification
import de.lucaspape.monstercat.core.music.util.artist
import de.lucaspape.monstercat.core.music.util.setCover
import de.lucaspape.monstercat.core.music.util.setCustomCover
import de.lucaspape.monstercat.core.music.util.title
import de.lucaspape.monstercat.core.util.parseSongToDB
import de.lucaspape.monstercat.core.music.util.BackgroundService
import de.lucaspape.monstercat.core.util.Settings
import org.json.JSONException

class StreamInfoUpdateAsync(
    private val context: Context
) : BackgroundService(500) {

    override fun background(): Boolean {
        val settings = Settings.getSettings(context)

        val artistTitleRequest = newLiveInfoRequest(context, {
            try {
                val songId =
                    parseSongToDB(it.getJSONObject("track"), context)

                if (songId != liveSongId && songId != null) {
                    liveSongId = songId

                    updateProgress(null)
                }
            } catch (e: JSONException) {
                try {
                    fallbackTitle = it.getString("title")
                    fallbackArtist = it.getString("artist")
                    fallbackVersion = it.getString("version")
                    fallbackCoverUrl = it.getString("coverUrl")
                    liveSongId = ""

                    updateProgress(null)
                } catch (e: JSONException) {

                }
            }
        }, {})

        val requestQueue =
            getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

        settings.getBoolean(context.getString(R.string.liveInfoSetting)).let {
            if (it == true && artistTitleRequest != null) {
                requestQueue.add(artistTitleRequest)
            } else {
                fallbackTitle = "Livestream"
                fallbackArtist = "Monstercat"
                fallbackVersion = ""
                fallbackCoverUrl = context.getString(R.string.fallbackCoverUrl)
                liveSongId = ""

                updateProgress(null)
            }
        }

        return true
    }

    override fun publishProgress(values: Array<String>?) {
        if (liveSongId != "") {
            setCover(
                context,
                liveSongId
            ) { bitmap ->
                updateNotification(
                    context,
                    liveSongId,
                    bitmap
                )
            }

            val songDatabaseHelper = SongDatabaseHelper(context)

            songDatabaseHelper.getSong(context, liveSongId)?.let { song ->
                title = "${song.title} ${song.version}"
                artist = song.artist
            }
        } else {
            setCustomCover(
                context,
                fallbackTitle + fallbackVersion + fallbackArtist,
                fallbackCoverUrl
            ) { bitmap ->
                updateNotification(
                    context,
                    fallbackTitle,
                    fallbackVersion,
                    fallbackArtist,
                    bitmap
                )
            }

            title = "$fallbackTitle $fallbackVersion"
            artist = fallbackArtist
        }
    }
}