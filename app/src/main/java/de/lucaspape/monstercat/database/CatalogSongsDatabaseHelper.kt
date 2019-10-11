package de.lucaspape.monstercat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.lang.IndexOutOfBoundsException

class CatalogSongsDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        @JvmStatic
        private val DATABASE_VERSION = 1
        @JvmStatic
        private val DATABASE_NAME = "catalog_songs_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS " + CatalogSongs.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(CatalogSongs.CREATE_TABLE)
    }

    fun insertSong(songId: Long): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(CatalogSongs.COLUMN_SONG_ID, songId)

        val id = db.insert(CatalogSongs.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getCatalogSongFromSongID(songId: Long): CatalogSongs? {

        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                CatalogSongs.TABLE_NAME, arrayOf(
                    CatalogSongs.COLUMN_ID,
                    CatalogSongs.COLUMN_SONG_ID
                ),
                CatalogSongs.COLUMN_SONG_ID + "=?",
                arrayOf(songId.toString()), null, null, null, null
            )

            cursor.moveToFirst()

            return try {
                val catalogSongs = CatalogSongs(
                    cursor.getInt(cursor.getColumnIndex(CatalogSongs.COLUMN_ID)),
                    cursor.getLong(cursor.getColumnIndex(CatalogSongs.COLUMN_SONG_ID))
                )

                cursor.close()

                catalogSongs
            } catch (e: IndexOutOfBoundsException) {
                cursor.close()
                null
            }
        } catch (e: SQLiteException) {
            return null
        }
    }

    fun getCatalogSong(id: Long): CatalogSongs? {

        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                CatalogSongs.TABLE_NAME, arrayOf(
                    CatalogSongs.COLUMN_ID
                ),
                CatalogSongs.COLUMN_ID + "=?",
                arrayOf(id.toString()), null, null, null, null
            )

            cursor.moveToFirst()

            return try {
                val catalogSongs = CatalogSongs(
                    cursor.getInt(cursor.getColumnIndex(CatalogSongs.COLUMN_ID)),
                    cursor.getLong(cursor.getColumnIndex(CatalogSongs.COLUMN_SONG_ID))
                )

                cursor.close()

                catalogSongs
            } catch (e: IndexOutOfBoundsException) {
                cursor.close()
                null
            }
        } catch (e: SQLiteException) {
            return null
        }
    }

    fun getAllSongs(): List<CatalogSongs> {
        val catalogSongs: ArrayList<CatalogSongs> = ArrayList()

        val selectQuery = "SELECT * FROM " + CatalogSongs.TABLE_NAME + " ORDER BY " +
                CatalogSongs.COLUMN_ID + " DESC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val catalogSong = CatalogSongs()
                catalogSong.id = cursor.getInt(cursor.getColumnIndex(CatalogSongs.COLUMN_ID))
                catalogSong.songId =
                    cursor.getLong(cursor.getColumnIndex(CatalogSongs.COLUMN_SONG_ID))

                catalogSongs.add(catalogSong)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return catalogSongs
    }

    @Suppress("unused")
    fun getSongsCount(): Int {
        val countQuery = "SELECT * FROM " + CatalogSongs.TABLE_NAME
        val db = readableDatabase
        val cursor = db.rawQuery(countQuery, null)

        val count = cursor.count
        cursor.close()

        return count
    }

    @Suppress("unused")
    fun deleteSong(catalogSongs: CatalogSongs) {
        val db = writableDatabase
        db.delete(
            CatalogSongs.TABLE_NAME,
            CatalogSongs.COLUMN_ID + " = ?",
            arrayOf(catalogSongs.id.toString())
        )
        db.close()
    }


}

