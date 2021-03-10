package de.lucaspape.monstercat.core.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.core.database.objects.PublicPlaylist
import java.lang.IndexOutOfBoundsException

class PublicPlaylistDatabaseHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {
    companion object {
        @JvmStatic
        val DATABASE_VERSION = 1001

        @JvmStatic
        private val DATABASE_NAME = "public_playlists_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS " + PublicPlaylist.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(PublicPlaylist.CREATE_TABLE)
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

        db?.execSQL("DROP TABLE IF EXISTS " + PublicPlaylist.TABLE_NAME)
        onCreate(db)
    }

    fun insertPlaylist(
        playlistId: String,
        name: String
    ): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(PublicPlaylist.COLUMN_PLAYLIST_ID, playlistId)
        values.put(PublicPlaylist.COLUMN_NAME, name)

        val id = db.insert(PublicPlaylist.TABLE_NAME, null, values)
        db.close()

        return id
    }

    fun getPlaylist(playlistId: String): PublicPlaylist? {
        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                PublicPlaylist.TABLE_NAME, arrayOf(
                    PublicPlaylist.COLUMN_ID,
                    PublicPlaylist.COLUMN_PLAYLIST_ID,
                    PublicPlaylist.COLUMN_NAME
                ),
                PublicPlaylist.COLUMN_PLAYLIST_ID + "=?",
                arrayOf(playlistId), null, null, null, null
            )

            cursor?.moveToFirst()

            try {

                val playlist = PublicPlaylist(
                    cursor.getInt(cursor.getColumnIndex(PublicPlaylist.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(PublicPlaylist.COLUMN_PLAYLIST_ID)),
                    cursor.getString(cursor.getColumnIndex(PublicPlaylist.COLUMN_NAME))
                )

                cursor.close()

                return playlist
            } catch (e: IndexOutOfBoundsException) {
                cursor.close()
                db.close()
                return null
            }

        } catch (e: SQLiteException) {
            return null
        }
    }

    fun getAllPlaylists(): ArrayList<PublicPlaylist> {
        val playlists: ArrayList<PublicPlaylist> = ArrayList()

        val selectQuery = "SELECT * FROM " + PublicPlaylist.TABLE_NAME + " ORDER BY " +
                PublicPlaylist.COLUMN_ID + " DESC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val playlist = PublicPlaylist(
                    cursor.getInt(cursor.getColumnIndex(PublicPlaylist.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(PublicPlaylist.COLUMN_PLAYLIST_ID)),
                    cursor.getString(cursor.getColumnIndex(PublicPlaylist.COLUMN_NAME))
                )

                playlists.add(playlist)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return playlists
    }
}