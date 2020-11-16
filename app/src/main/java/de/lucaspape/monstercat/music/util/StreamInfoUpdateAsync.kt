package de.lucaspape.monstercat.music.util

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.request.newLiveInfoRequest
import de.lucaspape.monstercat.util.getAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parseSongToDB
import de.lucaspape.util.BackgroundService
import de.lucaspape.util.Settings
import org.json.JSONException
import java.lang.ref.WeakReference

class StreamInfoUpdateAsync(
    private val contextReference: WeakReference<Context>
) : BackgroundService(500) {
    companion object {
        @JvmStatic
        var liveSongId = ""

        @JvmStatic
        var fallbackTitle = ""

        @JvmStatic
        var fallbackArtist = ""

        @JvmStatic
        var fallbackVersion = ""

        @JvmStatic
        var fallbackCoverUrl = ""
    }

    override fun background(): Boolean {
        contextReference.get()?.let { context ->
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
        
        return false
    }

    override fun publishProgress(values: Array<String>?) {
        contextReference.get()?.let { context ->
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
}