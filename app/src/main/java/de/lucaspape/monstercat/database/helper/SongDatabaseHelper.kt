package de.lucaspape.monstercat.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.database.Song
import java.lang.IndexOutOfBoundsException

class SongDatabaseHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {

    companion object {
        @JvmStatic
        private val DATABASE_VERSION = 2
        @JvmStatic
        private val DATABASE_NAME = "songs_db"
    }


    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS " + Song.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(Song.CREATE_TABLE)
    }

    fun insertSong(
        songId: String,
        title: String,
        version: String,
        albumId: String,
        artist: String,
        coverUrl: String
    ): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(Song.COLUMN_SONG_ID, songId)
        values.put(Song.COLUMN_TITLE, title)
        values.put(Song.COLUMN_VERSION, version)
        values.put(Song.COLUMN_ALBUM_ID, albumId)
        values.put(Song.COLUMN_ARTIST, artist)
        values.put(Song.COLUMN_COVER_URL, coverUrl)

        val id = db.insert(Song.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getSong(id: Long): Song {
        val db = readableDatabase

        val cursor = db.query(
            Song.TABLE_NAME, arrayOf(
                Song.COLUMN_ID,
                Song.COLUMN_SONG_ID,
                Song.COLUMN_TITLE,
                Song.COLUMN_VERSION,
                Song.COLUMN_ALBUM_ID,
                Song.COLUMN_ARTIST,
                Song.COLUMN_COVER_URL
            ),
            Song.COLUMN_ID + "=?",
            arrayOf(id.toString()), null, null, null, null
        )

        cursor?.moveToFirst()

        val song = Song(
            cursor.getInt(cursor.getColumnIndex(Song.COLUMN_ID)),
            cursor.getString(cursor.getColumnIndex(Song.COLUMN_SONG_ID)),
            cursor.getString(cursor.getColumnIndex(Song.COLUMN_TITLE)),
            cursor.getString(cursor.getColumnIndex(Song.COLUMN_VERSION)),
            cursor.getString(cursor.getColumnIndex(Song.COLUMN_ALBUM_ID)),
            cursor.getString(cursor.getColumnIndex(Song.COLUMN_ARTIST)),
            cursor.getString(cursor.getColumnIndex(Song.COLUMN_COVER_URL))
        )

        cursor.close()

        return song
    }

    fun getSong(songId: String): Song? {
        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                Song.TABLE_NAME, arrayOf(
                    Song.COLUMN_ID,
                    Song.COLUMN_SONG_ID,
                    Song.COLUMN_TITLE,
                    Song.COLUMN_VERSION,
                    Song.COLUMN_ALBUM_ID,
                    Song.COLUMN_ARTIST,
                    Song.COLUMN_COVER_URL
                ),
                Song.COLUMN_SONG_ID + "=?",
                arrayOf(songId), null, null, null, null
            )

            cursor?.moveToFirst()

            try {
                val song = Song(
                    cursor.getInt(cursor.getColumnIndex(Song.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_SONG_ID)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_VERSION)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_ALBUM_ID)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_ARTIST)),
                    cursor.getString(cursor.getColumnIndex(Song.COLUMN_COVER_URL))
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