package de.lucaspape.monstercat.core.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.core.database.objects.ManualPlaylist

class ManualPlaylistDatabaseHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {
    companion object {
        @JvmStatic
        val DATABASE_VERSION = 1001

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