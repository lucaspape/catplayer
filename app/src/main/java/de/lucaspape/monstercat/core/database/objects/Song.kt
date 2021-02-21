package de.lucaspape.monstercat.core.database.objects

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.util.wifiConnected
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.request.getAuthorizedRequestQueue
import de.lucaspape.monstercat.request.newLoadLivestreamUrlRequest
import java.io.File

data class Song(
    val context: Context,
    val songId: String,
    val title: String,
    val version: String,
    val albumId: String,
    val mcAlbumId: String,
    val artist: String,
    val artistId: String,
    val coverUrl: String,
    val isDownloadable: Boolean,
    val isStreamable: Boolean,
    val inEarlyAccess: Boolean,
    val creatorFriendly: Boolean
) {

    companion object {
        @JvmStatic
        val TABLE_NAME = "song"

        @JvmStatic
        val COLUMN_SONG_ID = "songId"

        @JvmStatic
        val COLUMN_TITLE = "title"

        @JvmStatic
        val COLUMN_VERSION = "version"

        @JvmStatic
        val COLUMN_ALBUM_ID = "albumId"

        @JvmStatic
        val COLUMN_ALBUM_MC_ID = "mcALbumId"

        @JvmStatic
        val COLUMN_ARTIST = "artist"

        @JvmStatic
        val COLUMN_ARTIST_ID = "artistId"

        @JvmStatic
        val COLUMN_COVER_URL = "coverUrl"

        @JvmStatic
        val COLUMN_DOWNLOADABLE = "downloadable"

        @JvmStatic
        val COLUMN_STREAMABLE = "streamable"

        @JvmStatic
        val COLUMN_INEARLYACCESS = "inEarlyAccess"

        @JvmStatic
        val COLUMN_CREATOR_FRIENDLY = "creatorFriendly"

        @JvmStatic
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_SONG_ID + " TEXT PRIMARY KEY," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_VERSION + " TEXT," +
                    COLUMN_ALBUM_ID + " TEXT," +
                    COLUMN_ALBUM_MC_ID + " TEXT," +
                    COLUMN_ARTIST + " TEXT," +
                    COLUMN_ARTIST_ID + " TEXT," +
                    COLUMN_COVER_URL + " TEXT," +
                    COLUMN_DOWNLOADABLE + " TEXT," +
                    COLUMN_STREAMABLE + " TEXT," +
                    COLUMN_INEARLYACCESS + " TEXT," +
                    COLUMN_CREATOR_FRIENDLY + " TEXT" +
                    ")"
    }

    val shownTitle = "$title $version"

    val downloadLocation: String
        get() = context.getExternalFilesDir(null)
            .toString() + "/" + artist + title + version + "." + Settings.getSettings(
            context
        ).getString("downloadType")

    val downloadUrl: String
        get() = if (Settings.getSettings(context)
                .getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && Settings.getSettings(
                context
            ).getBoolean(context.getString(R.string.customApiSupportsV1Setting)) == true
        ) {
            Settings.getSettings(context)
                .getString(context.getString(R.string.customDownloadUrlSetting)) + albumId + "/track-download/" + songId + "?format=" + Settings.getSettings(
                context
            ).getString("downloadType")
        } else {
            context.getString(R.string.trackContentUrl) + albumId + "/track-download/" + songId + "?format=" + Settings.getSettings(
                context
            ).getString("downloadType")
        }

    private val streamUrl: String
        get() = if (Settings.getSettings(context)
                .getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && Settings.getSettings(
                context
            ).getBoolean(context.getString(R.string.customApiSupportsV1Setting)) == true
        ) {
            Settings.getSettings(context)
                .getString(context.getString(R.string.customStreamUrlSetting)) + albumId + "/track-stream/" + songId
        } else {
            context.getString(R.string.trackContentUrl) + albumId + "/track-stream/" + songId
        }

    val downloaded: Boolean
        get() = File(downloadLocation).exists()

    fun getMediaSource(sid: String, cid: String, callback: (mediaSource: MediaSource?) -> Unit) {
        if (downloaded) {
            callback(fileToMediaSource(downloadLocation))
        } else {
            if (isStreamable) {
                urlToMediaSource(streamUrl, sid, cid, callback)
            } else {
                callback(null)
            }
        }
    }

    fun playbackAllowed(context: Context): Boolean {
        val networkAllowed = wifiConnected(context) == true || Settings.getSettings(
            context
        )
            .getBoolean(context.getString(R.string.streamOverMobileSetting)) == true || downloaded

        val blockNonCreatorFriendlySetting = Settings.getSettings(context)
            .getBoolean(context.getString(R.string.blockNonCreatorFriendlySetting))

        return if (!networkAllowed) {
            false
        } else if (creatorFriendly) {
            true
        } else blockNonCreatorFriendlySetting == false || blockNonCreatorFriendlySetting == null
    }

    private fun fileToMediaSource(fileLocation: String): ProgressiveMediaSource {
        return ProgressiveMediaSource.Factory(
            DefaultDataSourceFactory(
                context, Util.getUserAgent(
                    context, context.getString(R.string.applicationName)
                )
            )
        ).createMediaSource(MediaItem.fromUri(Uri.parse("file://$fileLocation")))
    }

    private fun urlToMediaSource(url: String, sid: String, cid: String, callback:(mediaSource:MediaSource)->Unit) {
        if(songId != "stream"){
            val httpSourceFactory =
                DefaultHttpDataSource.Factory()

            httpSourceFactory.setUserAgent(
                Util.getUserAgent(
                    context,
                    context.getString(R.string.applicationName)
                )
            )

            var cookie = ""

            if (cid != "") {
                cookie += "cid=$cid"
            }

            if (sid != "") {
                cookie += ";connect.sid=$sid"
            }

            if(cookie.isNotEmpty()){
                httpSourceFactory.setDefaultRequestProperties(mapOf("Cookie" to cookie))
            }

            callback(ProgressiveMediaSource.Factory(httpSourceFactory)
                .createMediaSource(MediaItem.fromUri(url.toUri())))

        }else{
            getStreamUrl(context, {
                callback(
                    HlsMediaSource.Factory(
                    DefaultDataSourceFactory(
                        context, context.getString(R.string.livestreamUserAgent)
                    )
                ).createMediaSource(MediaItem.fromUri(it.toUri())))
            }, {})
        }
    }

    private fun getStreamUrl(
        context: Context,
        callback: (liveStreamUrl: String) -> Unit,
        errorCallback: () -> Unit
    ) {
        val requestQueue = getAuthorizedRequestQueue(context, context.getString(R.string.customApiBaseUrlSetting))

        val loadLiveStreamRequest = newLoadLivestreamUrlRequest(context, {
            callback(it.getString("monstercat"))
        }, {
            errorCallback()
        })

        if(loadLiveStreamRequest != null){
            requestQueue.add(loadLiveStreamRequest)
        }else{
            errorCallback()
        }
    }
}