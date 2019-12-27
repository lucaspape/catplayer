package de.lucaspape.monstercat.database

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.Settings
import java.io.File
import java.lang.ref.WeakReference

data class Song(
    val context: Context,
    val id: Int,
    val songId: String,
    val title: String,
    val version: String,
    val albumId: String,
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
        val COLUMN_ID = "id"
        @JvmStatic
        val COLUMN_SONG_ID = "songId"
        @JvmStatic
        val COLUMN_TITLE = "title"
        @JvmStatic
        val COLUMN_VERSION = "version"
        @JvmStatic
        val COLUMN_ALBUM_ID = "albumId"
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
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_SONG_ID + " TEXT," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_VERSION + " TEXT," +
                    COLUMN_ALBUM_ID + " TEXT," +
                    COLUMN_ARTIST + " TEXT," +
                    COLUMN_COVER_URL + " TEXT," +
                    COLUMN_DOWNLOADABLE + " TEXT," +
                    COLUMN_STREAMABLE + " TEXT," +
                    COLUMN_INEARLYACCESS + " TEXT" +
                    ")"
    }

    val downloadLocation: String =
        context.getExternalFilesDir(null).toString() + "/" + artist + title + version + "." + Settings(
            context
        ).getSetting("downloadType")
    val streamLocation: String =
        context.getString(R.string.trackContentUrl) + albumId + "/track-stream/" + songId
    val streamDownloadLocation: String = "$downloadLocation.stream"

    fun getUrl(): String {
        return if(File(downloadLocation).exists() && File(streamDownloadLocation).exists()){
            File(streamDownloadLocation).delete()

            downloadLocation
        }else if(File(downloadLocation).exists()){
            downloadLocation
        }else if(File(streamDownloadLocation).exists()){
            streamDownloadLocation
        }else{
            streamLocation
        }
    }
}