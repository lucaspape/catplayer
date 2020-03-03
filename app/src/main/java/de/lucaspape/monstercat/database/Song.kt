package de.lucaspape.monstercat.database

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.Settings
import de.lucaspape.monstercat.util.sid
import java.io.File

data class Song(
    val context: Context,
    val songId: String,
    val title: String,
    val version: String,
    val albumId: String,
    val mcAlbumId: String,
    val artist: String,
    val coverUrl: String,
    val isDownloadable: Boolean,
    val isStreamable: Boolean,
    val inEarlyAccess: Boolean
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
        val COLUMN_COVER_URL = "coverUrl"

        @JvmStatic
        val COLUMN_DOWNLOADABLE = "downloadable"

        @JvmStatic
        val COLUMN_STREAMABLE = "streamable"

        @JvmStatic
        val COLUMN_INEARLYACCESS = "inEarlyAccess"

        @JvmStatic
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_SONG_ID + " TEXT PRIMARY KEY," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_VERSION + " TEXT," +
                    COLUMN_ALBUM_ID + " TEXT," +
                    COLUMN_ALBUM_MC_ID + " TEXT," +
                    COLUMN_ARTIST + " TEXT," +
                    COLUMN_COVER_URL + " TEXT," +
                    COLUMN_DOWNLOADABLE + " TEXT," +
                    COLUMN_STREAMABLE + " TEXT," +
                    COLUMN_INEARLYACCESS + " TEXT" +
                    ")"
    }

    val shownTitle = "$title $version"

    val downloadLocation: String =
        context.getExternalFilesDir(null)
            .toString() + "/" + artist + title + version + "." + Settings(
            context
        ).getSetting("downloadType")

    val downloadUrl =
        context.getString(R.string.trackContentUrl) + albumId + "/track-download/" + songId + "?format=" + Settings(
            context
        ).getSetting("downloadType")

    val streamUrl: String =
        context.getString(R.string.trackContentUrl) + albumId + "/track-stream/" + songId

    val streamDownloadLocation: String = "$downloadLocation.stream"

    fun getSongDownloadStatus(): String {
        return when {
            File(downloadLocation).exists() -> {
                "android.resource://de.lucaspape.monstercat/drawable/ic_offline_pin_green_24dp"
            }
            File(streamDownloadLocation).exists() -> {
                "android.resource://de.lucaspape.monstercat/drawable/ic_offline_pin_orange_24dp"
            }
            else -> {
                "android.resource://de.lucaspape.monstercat/drawable/ic_file_download_24dp"
            }
        }
    }

    fun getUrl(): String {
        return if (File(downloadLocation).exists() && File(streamDownloadLocation).exists()) {
            File(streamDownloadLocation).delete()

            downloadLocation
        } else if (File(downloadLocation).exists()) {
            downloadLocation
        } else if (File(streamDownloadLocation).exists()) {
            streamDownloadLocation
        } else {
            streamUrl
        }
    }

    fun getMediaSource(): MediaSource? {
        return if (File(downloadLocation).exists() && File(streamDownloadLocation).exists()) {
            File(streamDownloadLocation).delete()

            fileToMediaSource(downloadLocation)
        } else if (File(downloadLocation).exists()) {
            fileToMediaSource(downloadLocation)
        } else if (File(streamDownloadLocation).exists()) {
            fileToMediaSource(streamDownloadLocation)
        } else {
            if (isStreamable) {
                urlToMediaSource(streamUrl)
            } else {
                null
            }
        }
    }

    private fun fileToMediaSource(fileLocation: String): ProgressiveMediaSource {
        return ProgressiveMediaSource.Factory(
            DefaultDataSourceFactory(
                context, Util.getUserAgent(
                    context, context.getString(R.string.applicationName)
                )
            )
        ).createMediaSource(Uri.parse("file://$fileLocation"))
    }

    private fun urlToMediaSource(url: String): ProgressiveMediaSource {
        val httpSourceFactory =
            DefaultHttpDataSourceFactory(
                Util.getUserAgent(
                    context,
                    context.getString(R.string.applicationName)
                )
            )

        httpSourceFactory.defaultRequestProperties.set("Cookie", "connect.sid=$sid")

        return ProgressiveMediaSource.Factory(httpSourceFactory)
            .createMediaSource(url.toUri())
    }
}