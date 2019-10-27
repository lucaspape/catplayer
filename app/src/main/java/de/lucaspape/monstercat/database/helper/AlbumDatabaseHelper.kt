package de.lucaspape.monstercat.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.database.Album
import java.lang.IndexOutOfBoundsException

class AlbumDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {
    companion object {
        @JvmStatic
        private val DATABASE_VERSION = 2
        @JvmStatic
        private val DATABASE_NAME = "albums_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS " + Album.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(Album.CREATE_TABLE)
    }

    fun reCreateTable(){
        val db = writableDatabase

        db!!.execSQL("DROP TABLE IF EXISTS " + Album.TABLE_NAME)
        onCreate(db)
    }

    fun insertAlbum(albumId: String, title: String, artist: String, coverUrl: String): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(Album.COLUMN_TITLE, title)
        values.put(Album.COLUMN_ALBUM_ID, albumId)
        values.put(Album.COLUMN_ARTIST, artist)
        values.put(Album.COLUMN_COVER_URL, coverUrl)

        val id = db.insert(Album.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getAlbum(albumId: String): Album? {
        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                Album.TABLE_NAME, arrayOf(
                    Album.COLUMN_ID,
                    Album.COLUMN_ALBUM_ID,
                    Album.COLUMN_TITLE,
                    Album.COLUMN_ARTIST,
                    Album.COLUMN_COVER_URL
                ),
                Album.COLUMN_ALBUM_ID + "=?",
                arrayOf(albumId), null, null, null, null
            )

            cursor?.moveToFirst()

            try {
                val album = Album(
                    cursor.getInt(cursor.getColumnIndex(Album.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ALBUM_ID)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ARTIST)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_COVER_URL))
                )

                cursor.close()

                return album
            } catch (e: IndexOutOfBoundsException) {
                cursor.close()
                db.close()
                return null
            }

        } catch (e: SQLiteException) {
            return null
        }
    }

    fun getAllAlbums(): List<Album> {
        val albums: ArrayList<Album> = ArrayList()

        val selectQuery = "SELECT * FROM " + Album.TABLE_NAME + " ORDER BY " +
                Album.COLUMN_ID + " DESC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val album = Album(
                    cursor.getInt(cursor.getColumnIndex(Album.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ALBUM_ID)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ARTIST)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_COVER_URL))
                )
                albums.add(album)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return albums
    }
}