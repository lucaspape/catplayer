package de.lucaspape.monstercat.music.util

import android.content.Context
import androidx.lifecycle.ViewModel
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.request.newLiveInfoRequest
import de.lucaspape.monstercat.util.getAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parseSongToDB
import de.lucaspape.util.Settings
import kotlinx.coroutines.*
import org.json.JSONException
import java.lang.ref.WeakReference

class StreamInfoUpdateAsync(
    private val contextReference: WeakReference<Context>
):ViewModel() {
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

    private val viewModelJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var lastActive: Long = 0

    val active: Boolean
        get() {
            return System.currentTimeMillis() - lastActive <= 1000
        }

    fun execute() {
        scope.launch {
            withContext(Dispatchers.Default){
                contextReference.get()?.let { context ->
                    val settings = Settings.getSettings(context)

                    val artistTitleRequest = newLiveInfoRequest(context, {
                        try {
                            val songId =
                                parseSongToDB(it.getJSONObject("track"), context)

                            if (songId != liveSongId && songId != null) {
                                liveSongId = songId

                                scope.launch {
                                    publishProgress()
                                }
                            }
                        } catch (e: JSONException) {
                            try {
                                fallbackTitle = it.getString("title")
                                fallbackArtist = it.getString("artist")
                                fallbackVersion = it.getString("version")
                                fallbackCoverUrl = it.getString("coverUrl")
                                liveSongId = ""

                                scope.launch {
                                    publishProgress()
                                }
                            } catch (e: JSONException) {

                            }
                        }
                    }, {})

                    val requestQueue =
                        getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

                    while (true) {
                        lastActive = System.currentTimeMillis()

                        settings.getBoolean(context.getString(R.string.liveInfoSetting)).let {
                            if(it == true && artistTitleRequest != null){
                                requestQueue.add(artistTitleRequest)
                            }else{
                                fallbackTitle = "Livestream"
                                fallbackArtist = "Monstercat"
                                fallbackVersion = ""
                                fallbackCoverUrl = context.getString(R.string.fallbackCoverUrl)
                                liveSongId = ""

                                publishProgress()
                            }
                        }

                        delay(500)
                    }
                }
            }
        }
    }

    private suspend fun publishProgress() {
        withContext(Dispatchers.Main){
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
                        updateNotification(context, fallbackTitle, fallbackVersion, fallbackArtist, bitmap)
                    }

                    title = "$fallbackTitle $fallbackVersion"
                    artist = fallbackArtist
                }
            }
        }
    }

    fun destroy() {
        scope.cancel()
    }
}