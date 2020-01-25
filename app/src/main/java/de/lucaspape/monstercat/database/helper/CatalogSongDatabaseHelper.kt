package de.lucaspape.monstercat.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.database.CatalogSong
import java.lang.IndexOutOfBoundsException

class CatalogSongDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {

    companion object {
        @JvmStatic
        private val DATABASE_VERSION = 3
        @JvmStatic
        private val DATABASE_NAME = "catalog_songs_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS " + CatalogSong.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CatalogSong.CREATE_TABLE)
    }

    fun reCreateTable(){
        val db = writableDatabase

        db?.execSQL("DROP TABLE IF EXISTS " + CatalogSong.TABLE_NAME)
        onCreate(db)
    }

    fun insertSong(songId: Long): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(CatalogSong.COLUMN_SONG_ID, songId)

        val id = db.insert(CatalogSong.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getCatalogSong(songId: Long): CatalogSong? {

        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                CatalogSong.TABLE_NAME, arrayOf(
                    CatalogSong.COLUMN_ID,
                    CatalogSong.COLUMN_SONG_ID
                ),
                CatalogSong.COLUMN_SONG_ID + "=?",
                arrayOf(songId.toString()), null, null, null, null
            )

            cursor.moveToFirst()

            return try {
                val catalogSongs = CatalogSong(
                    cursor.getInt(cursor.getColumnIndex(CatalogSong.COLUMN_ID)),
                    cursor.getLong(cursor.getColumnIndex(CatalogSong.COLUMN_SONG_ID))
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

    fun getSongs(skip:Long, limit:Long): List<CatalogSong>{
        val catalogSongs: ArrayList<CatalogSong> = ArrayList()

        val selectQuery = "SELECT * FROM " + CatalogSong.TABLE_NAME + " ORDER BY " +
                CatalogSong.COLUMN_ID + " ASC LIMIT $skip,$limit"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val catalogSong = CatalogSong(
                    cursor.getInt(cursor.getColumnIndex(CatalogSong.COLUMN_ID)),
                    cursor.getLong(cursor.getColumnIndex(CatalogSong.COLUMN_SONG_ID))
                )

                catalogSongs.add(catalogSong)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        catalogSongs.reverse()

        return catalogSongs
    }
}

