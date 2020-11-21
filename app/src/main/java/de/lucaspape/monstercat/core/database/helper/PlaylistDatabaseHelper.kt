package de.lucaspape.monstercat.core.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.core.database.objects.Playlist
import java.lang.IndexOutOfBoundsException

class PlaylistDatabaseHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {
    companion object {
        @JvmStatic
        val DATABASE_VERSION = 5 * SongDatabaseHelper.DATABASE_VERSION

        @JvmStatic
        private val DATABASE_NAME = "playlists_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS " + Playlist.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(Playlist.CREATE_TABLE)
    }

    fun reCreateTable(context: Context, reCreateItems: Boolean) {
        if (reCreateItems) {
            val allPlaylists = getAllPlaylists()

            for (playlist in allPlaylists) {
                val playlistItemDatabaseHelper =
                    PlaylistItemDatabaseHelper(context, playlist.playlistId)
                playlistItemDatabaseHelper.reCreateTable()
            }
        }

        val db = writableDatabase

        db?.execSQL("DROP TABLE IF EXISTS " + Playlist.TABLE_NAME)
        onCreate(db)
    }

    fun insertPlaylist(
        playlistId: String,
        name: String,
        ownPlaylist: Boolean,
        public: Boolean
    ): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(Playlist.COLUMN_PLAYLIST_ID, playlistId)
        values.put(Playlist.COLUMN_NAME, name)
        values.put(Playlist.COLUMN_OWN_PLAYLIST, ownPlaylist.toString())
        values.put(Playlist.COLUMN_PUBLIC, public.toString())

        val id = db.insert(Playlist.TABLE_NAME, null, values)
        db.close()

        return id
    }

    fun getPlaylist(playlistId: String): Playlist? {
        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                Playlist.TABLE_NAME, arrayOf(
                    Playlist.COLUMN_ID,
                    Playlist.COLUMN_PLAYLIST_ID,
                    Playlist.COLUMN_NAME,
                    Playlist.COLUMN_OWN_PLAYLIST,
                    Playlist.COLUMN_PUBLIC
                ),
                Playlist.COLUMN_PLAYLIST_ID + "=?",
                arrayOf(playlistId), null, null, null, null
            )

            cursor?.moveToFirst()

            try {

                val playlist = Playlist(
                    cursor.getInt(cursor.getColumnIndex(Playlist.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_PLAYLIST_ID)),
                    cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_OWN_PLAYLIST))!!
                        .toBoolean(),
                    cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_PUBLIC))!!
                        .toBoolean()
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

    fun getAllPlaylists(): List<Playlist> {
        val playlists: ArrayList<Playlist> = ArrayList()

        val selectQuery = "SELECT * FROM " + Playlist.TABLE_NAME + " ORDER BY " +
                Playlist.COLUMN_ID + " DESC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val playlist = Playlist(
                    cursor.getInt(cursor.getColumnIndex(Playlist.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_PLAYLIST_ID)),
                    cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_OWN_PLAYLIST))!!
                        .toBoolean(),
                    cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_PUBLIC))!!.toBoolean()
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
        db.delete(Playlist.TABLE_NAME, Playlist.COLUMN_PLAYLIST_ID + "=?", arrayOf(playlistId))
        db.close()
    }
}