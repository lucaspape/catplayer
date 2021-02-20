package de.lucaspape.monstercat.core.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.core.database.objects.ManualPlaylist
import java.lang.IndexOutOfBoundsException

class ManualPlaylistDatabaseHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {
    companion object {
        @JvmStatic
        val DATABASE_VERSION = 4 * SongDatabaseHelper.DATABASE_VERSION

        @JvmStatic
        private val DATABASE_NAME = "manual_playlists_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS " + ManualPlaylist.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(ManualPlaylist.CREATE_TABLE)
    }

    fun reCreateTable(context: Context, reCreateItems: Boolean) {
        if (reCreateItems) {
            val allPlaylists = getAllPlaylists()

            for (playlist in allPlaylists) {
                val playlistItemDatabaseHelper =
                    ItemDatabaseHelper(context, playlist.playlistId)
                playlistItemDatabaseHelper.reCreateTable()
            }
        }

        val db = writableDatabase

        db?.execSQL("DROP TABLE IF EXISTS " + ManualPlaylist.TABLE_NAME)
        onCreate(db)
    }

    fun insertPlaylist(playlistId: String): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(ManualPlaylist.COLUMN_PLAYLIST_ID, playlistId)

        val id = db.insert(ManualPlaylist.TABLE_NAME, null, values)
        db.close()

        return id
    }

    fun getPlaylist(playlistId: String): ManualPlaylist? {
        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                ManualPlaylist.TABLE_NAME, arrayOf(
                    ManualPlaylist.COLUMN_ID,
                    ManualPlaylist.COLUMN_PLAYLIST_ID
                ),
                ManualPlaylist.COLUMN_PLAYLIST_ID + "=?",
                arrayOf(playlistId), null, null, null, null
            )

            cursor?.moveToFirst()

            return try {
                val playlist = ManualPlaylist(
                    cursor.getInt(cursor.getColumnIndex(ManualPlaylist.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(ManualPlaylist.COLUMN_PLAYLIST_ID))
                )

                cursor.close()

                playlist
            } catch (e: IndexOutOfBoundsException) {
                cursor.close()
                db.close()
                null
            }

        } catch (e: SQLiteException) {
            return null
        }
    }

    fun getAllPlaylists(): List<ManualPlaylist> {
        val playlists: ArrayList<ManualPlaylist> = ArrayList()

        val selectQuery = "SELECT * FROM " + ManualPlaylist.TABLE_NAME + " ORDER BY " +
                ManualPlaylist.COLUMN_ID + " DESC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val playlist = ManualPlaylist(
                    cursor.getInt(cursor.getColumnIndex(ManualPlaylist.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(ManualPlaylist.COLUMN_PLAYLIST_ID))
                )

                playlists.add(playlist)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return playlists
    }

    fun removePlaylist(playlistId: String) {
        val db = writableDatabase
        db.delete(
            ManualPlaylist.TABLE_NAME,
            ManualPlaylist.COLUMN_PLAYLIST_ID + "=?",
            arrayOf(playlistId)
        )
        db.close()
    }
}