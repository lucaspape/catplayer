package de.lucaspape.monstercat.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.database.AlbumItem

class AlbumItemDatabaseHelper(context: Context, var albumId: String) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {

    companion object {
        @JvmStatic
        private val DATABASE_VERSION = 2 * AlbumDatabaseHelper.DATABASE_VERSION
        @JvmStatic
        private val DATABASE_NAME = "album_items_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(
            "DROP TABLE IF EXISTS " + AlbumItem(
                albumId,
                0,
                0
            ).TABLE_NAME
        )
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase?) {
        db?.execSQL(AlbumItem(albumId, 0, 0).CREATE_TABLE)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(AlbumItem(albumId, 0, 0).CREATE_TABLE)
    }

    fun reCreateTable() {
        val db = writableDatabase

        db?.execSQL(
            "DROP TABLE IF EXISTS " + AlbumItem(
                albumId,
                0,
                0
            ).TABLE_NAME
        )
        onCreate(db)
    }

    fun insertSongId(songId: Long): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(AlbumItem.COLUMN_SONG_ID, songId)

        val id = db.insert(AlbumItem(albumId, 0, 0).TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getItemFromSongId(songId: Long): AlbumItem? {
        val db = readableDatabase

        try {
            val cursor = db.query(
                AlbumItem(albumId, 0, 0).TABLE_NAME, arrayOf(
                    AlbumItem.COLUMN_ID,
                    AlbumItem.COLUMN_SONG_ID
                ),
                AlbumItem.COLUMN_SONG_ID + "=?",
                arrayOf(songId.toString()), null, null, null, null
            )

            cursor?.moveToFirst()

            return try {
                val albumItem = AlbumItem(
                    albumId,
                    cursor.getLong(cursor.getColumnIndex(AlbumItem.COLUMN_ID)),
                    cursor.getLong(cursor.getColumnIndex(AlbumItem.COLUMN_SONG_ID))
                )

                cursor.close()

                albumItem
            } catch (e: IndexOutOfBoundsException) {
                cursor.close()
                db.close()
                null
            }
        } catch (e: SQLiteException) {
            return null
        }
    }

    fun getAllData(): List<AlbumItem> {
        val albumItems: ArrayList<AlbumItem> = ArrayList()

        val selectQuery = "SELECT * FROM " + AlbumItem(
            albumId,
            0,
            0
        ).TABLE_NAME + " ORDER BY " +
                AlbumItem.COLUMN_ID + " ASC "

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val newAlbumItem = AlbumItem(
                    albumId,
                    cursor.getLong(cursor.getColumnIndex(AlbumItem.COLUMN_ID)),
                    cursor.getLong(cursor.getColumnIndex(AlbumItem.COLUMN_SONG_ID))
                )

                albumItems.add(newAlbumItem)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return albumItems
    }

}