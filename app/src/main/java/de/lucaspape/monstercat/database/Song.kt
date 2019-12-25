package de.lucaspape.monstercat.database

import java.io.File

data class Song(
    val id: Int,
    val songId: String,
    val title: String,
    val version: String,
    val albumId: String,
    val artist: String,
    val coverUrl: String
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
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_SONG_ID + " TEXT," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_VERSION + " TEXT," +
                    COLUMN_ALBUM_ID + " TEXT," +
                    COLUMN_ARTIST + " TEXT," +
                    COLUMN_COVER_URL + " TEXT" +
                    ")"
    }

    var downloadLocation: String = ""
    var streamLocation: String = ""

    fun getUrl(): String {
        return if (File(downloadLocation).exists()) {
            downloadLocation
        } else if(File("$downloadLocation.stream").exists()){
            return "$downloadLocation.stream"
        }else{
            streamLocation
        }
    }

}