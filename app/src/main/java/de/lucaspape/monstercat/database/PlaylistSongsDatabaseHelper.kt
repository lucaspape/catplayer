package de.lucaspape.monstercat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.lang.IndexOutOfBoundsException

class PlaylistSongsDatabaseHelper(context: Context, var playlistId: String) :
    SQLiteOpenHelper(context, "playlists_data_db_" + playlistId, null, DATABASE_VERSION) {

    companion object {
        @JvmStatic
        private val DATABASE_VERSION = 1
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS " + PlaylistSongs.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(PlaylistSongs.CREATE_TABLE)
    }

    fun insertSongId(songId: Long): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(PlaylistSongs.COLUMN_SONG_ID, songId)

        val id = db.insert(PlaylistSongs.TABLE_NAME, null, values)
        db.close()
        return id

    }

    fun getPlaylistData(id: Int): PlaylistSongs? {
        val db = readableDatabase

        try {
            try {
                val cursor = db.query(
                    PlaylistSongs.TABLE_NAME, arrayOf(
                        PlaylistSongs.COLUMN_ID,
                        PlaylistSongs.COLUMN_SONG_ID
                    ),
                    PlaylistSongs.COLUMN_ID + "=?",
                    arrayOf(id.toString()), null, null, null, null
                )

                cursor?.moveToFirst()

                val newPlaylistData = PlaylistSongs(
                    cursor.getInt(cursor.getColumnIndex(PlaylistSongs.COLUMN_ID)),
                    cursor.getLong(cursor.getColumnIndex(PlaylistSongs.COLUMN_SONG_ID))
                )

                cursor.close()

                return newPlaylistData
            } catch (e: IndexOutOfBoundsException) {
                return null
            }
        } catch (e: SQLiteException) {
            return null
        }
    }

    // @Suppress("unused")
    fun getPlaylistData(songId: Long): PlaylistSongs? {
        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                PlaylistSongs.TABLE_NAME, arrayOf(
                    PlaylistSongs.COLUMN_ID,
                    PlaylistSongs.COLUMN_SONG_ID
                ),
                Song.COLUMN_SONG_ID + "=?",
                arrayOf(songId.toString()), null, null, null, null
            )

            cursor?.moveToFirst()

            try {
                val newPlaylistData = PlaylistSongs(
                    cursor.getInt(cursor.getColumnIndex(Playlist.COLUMN_ID)),
                    cursor.getLong(cursor.getColumnIndex(PlaylistSongs.COLUMN_SONG_ID))
                )

                cursor.close()

                return newPlaylistData
            } catch (e: IndexOutOfBoundsException) {
                cursor.close()
                db.close()
                return null
            }

        } catch (e: SQLiteException) {
            return null
        }
    }

    fun getAllData(): List<PlaylistSongs> {
        val playlistDatas: ArrayList<PlaylistSongs> = ArrayList()

        val selectQuery = "SELECT * FROM " + PlaylistSongs.TABLE_NAME + " ORDER BY " +
                PlaylistSongs.COLUMN_ID + " DESC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val newPlaylistData = PlaylistSongs()
                newPlaylistData.id = cursor.getInt(cursor.getColumnIndex(PlaylistSongs.COLUMN_ID))
                newPlaylistData.songId =
                    cursor.getLong(cursor.getColumnIndex(PlaylistSongs.COLUMN_SONG_ID))

                playlistDatas.add(newPlaylistData)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return playlistDatas
    }

    @Suppress("unused")
    fun getPlaylistDataCount(): Int {
        val countQuery = "SELECT * FROM " + PlaylistSongs.TABLE_NAME
        val db = readableDatabase
        val cursor = db.rawQuery(countQuery, null)

        val count = cursor.count
        cursor.close()

        return count
    }

    @Suppress("unused")
    fun deletePlaylistData(dPlaylistData: PlaylistSongs) {
        val db = writableDatabase
        db.delete(
            PlaylistSongs.TABLE_NAME,
            PlaylistSongs.COLUMN_ID + " = ?",
            arrayOf(dPlaylistData.id.toString())
        )
        db.close()
    }

}