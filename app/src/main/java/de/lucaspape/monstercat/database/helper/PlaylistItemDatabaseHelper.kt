package de.lucaspape.monstercat.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.database.objects.PlaylistItem

class PlaylistItemDatabaseHelper(context: Context, var playlistId: String) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {

    companion object {
        @JvmStatic
        private val DATABASE_VERSION = 3 * PlaylistDatabaseHelper.DATABASE_VERSION

        @JvmStatic
        private val DATABASE_NAME = "playlist_items_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(
            "DROP TABLE IF EXISTS " + PlaylistItem(
                playlistId,
                0,
                ""
            ).TABLE_NAME
        )
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase?) {
        db?.execSQL(
            PlaylistItem(
                playlistId,
                0,
                ""
            ).CREATE_TABLE)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            PlaylistItem(
                playlistId,
                0,
                ""
            ).CREATE_TABLE)
    }

    fun reCreateTable() {
        val db = writableDatabase

        db?.execSQL(
            "DROP TABLE IF EXISTS " + PlaylistItem(
                playlistId,
                0,
                ""
            ).TABLE_NAME
        )
        onCreate(db)
    }

    fun insertSongId(songId: String): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(PlaylistItem.COLUMN_SONG_ID, songId)

        val id = db.insert(
            PlaylistItem(
                playlistId,
                0,
                ""
            ).TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getAllData(): List<PlaylistItem> {
        val playlistItems: ArrayList<PlaylistItem> = ArrayList()

        val selectQuery = "SELECT * FROM " + PlaylistItem(
            playlistId,
            0,
            ""
        ).TABLE_NAME + " ORDER BY " +
                PlaylistItem.COLUMN_ID + " ASC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val newPlaylistItem =
                    PlaylistItem(
                        playlistId,
                        cursor.getLong(cursor.getColumnIndex(PlaylistItem.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(PlaylistItem.COLUMN_SONG_ID))
                    )

                playlistItems.add(newPlaylistItem)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return playlistItems
    }

}