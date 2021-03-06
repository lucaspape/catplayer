package de.lucaspape.monstercat.core.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.core.database.objects.Song
import java.lang.IndexOutOfBoundsException

class SongDatabaseHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {

    companion object {
        @JvmStatic
        val DATABASE_VERSION = 1002

        @JvmStatic
        private val DATABASE_NAME = "songs_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS " + Song.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(Song.CREATE_TABLE)
    }

    fun insertSong(
        songId: String,
        title: String,
        version: String,
        albumId: String,
        mcAlbumId: String,
        artist: String,
        artistId: String,
        downloadable: Boolean,
        streamable: Boolean,
        inEarlyAccess: Boolean,
        creatorFriendly: Boolean,
        explicit: Boolean
    ): String {
        val values = ContentValues()

        values.put(Song.COLUMN_SONG_ID, songId)
        values.put(Song.COLUMN_TITLE, title)
        values.put(Song.COLUMN_VERSION, version)
        values.put(Song.COLUMN_ALBUM_ID, albumId)
        values.put(Song.COLUMN_ALBUM_MC_ID, mcAlbumId)
        values.put(Song.COLUMN_ARTIST, artist)
        values.put(Song.COLUMN_ARTIST_ID, artistId)
        values.put(Song.COLUMN_DOWNLOADABLE, downloadable.toString())
        values.put(Song.COLUMN_STREAMABLE, streamable.toString())
        values.put(Song.COLUMN_IN_EARLY_ACCESS, inEarlyAccess.toString())
        values.put(Song.COLUMN_CREATOR_FRIENDLY, creatorFriendly.toString())
        values.put(Song.COLUMN_EXPLICIT, explicit.toString())

        if (getSong(songId) == null) {
            val db = writableDatabase
            db.insert(Song.TABLE_NAME, null, values)

            db.close()
        } else {
            val db = writableDatabase
            db.update(
                Song.TABLE_NAME,
                values,
                Song.COLUMN_SONG_ID + "=\'" + songId + "\'",
                null
            )

            db.close()
        }

        return songId
    }

    fun getSong(songId: String): Song? {
        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                Song.TABLE_NAME, arrayOf(
                    Song.COLUMN_SONG_ID,
                    Song.COLUMN_TITLE,
                    Song.COLUMN_VERSION,
                    Song.COLUMN_ALBUM_ID,
                    Song.COLUMN_ALBUM_MC_ID,
                    Song.COLUMN_ARTIST,
                    Song.COLUMN_ARTIST_ID,
                    Song.COLUMN_DOWNLOADABLE,
                    Song.COLUMN_STREAMABLE,
                    Song.COLUMN_IN_EARLY_ACCESS,
                    Song.COLUMN_CREATOR_FRIENDLY,
                    Song.COLUMN_EXPLICIT
                ),
                Song.COLUMN_SONG_ID + "=?",
                arrayOf(songId), null, null, null, null
            )

            cursor?.moveToFirst()

            try {
                val song = Song(
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_SONG_ID)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_VERSION)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_ALBUM_ID)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_ALBUM_MC_ID)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_ARTIST)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_ARTIST_ID)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_DOWNLOADABLE))!!
                        .toBoolean(),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_STREAMABLE))!!
                        .toBoolean(),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_IN_EARLY_ACCESS))!!
                        .toBoolean(),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_CREATOR_FRIENDLY))!!
                        .toBoolean(),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_EXPLICIT))!!
                        .toBoolean()
                )

                cursor.close()

                return song
            } catch (e: IndexOutOfBoundsException) {
                cursor.close()
                db.close()
                return null
            }

        } catch (e: SQLiteException) {
            return null
        }
    }
}