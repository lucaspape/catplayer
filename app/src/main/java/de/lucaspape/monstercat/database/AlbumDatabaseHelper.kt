package de.lucaspape.monstercat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.lang.IndexOutOfBoundsException

class AlbumDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        @JvmStatic
        private val DATABASE_VERSION = 1
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

    fun getAlbum(id: Long): Album {
        val db = readableDatabase

        val cursor = db.query(
            Album.TABLE_NAME, arrayOf(
                Album.COLUMN_ID,
                Album.COLUMN_ALBUM_ID,
                Album.COLUMN_TITLE,
                Album.COLUMN_ARTIST,
                Album.COLUMN_COVER_URL
            ),
            Song.COLUMN_ID + "=?",
            arrayOf(id.toString()), null, null, null, null
        )

        cursor?.moveToFirst()

        val album = Album(
            cursor.getInt(cursor.getColumnIndex(Album.COLUMN_ID)),
            cursor.getString(cursor.getColumnIndex(Album.COLUMN_ALBUM_ID)),
            cursor.getString(cursor.getColumnIndex(Album.COLUMN_TITLE)),
            cursor.getString(cursor.getColumnIndex(Album.COLUMN_ARTIST)),
            cursor.getString(cursor.getColumnIndex(Album.COLUMN_COVER_URL))
        )

        cursor.close()

        return album
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
                Album.COLUMN_ID + " ASC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val album = Album()
                album.id = cursor.getInt(cursor.getColumnIndex(Album.COLUMN_ID))
                album.albumId = cursor.getString(cursor.getColumnIndex(Album.COLUMN_ALBUM_ID))
                album.title = cursor.getString(cursor.getColumnIndex(Album.COLUMN_TITLE))
                album.artist = cursor.getString(cursor.getColumnIndex(Album.COLUMN_ARTIST))
                album.coverUrl = cursor.getString(cursor.getColumnIndex(Album.COLUMN_COVER_URL))

                albums.add(album)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return albums
    }

    @Suppress("unused")
    fun getAlbumsCount(): Int {
        val countQuery = "SELECT * FROM " + Album.TABLE_NAME
        val db = readableDatabase
        val cursor = db.rawQuery(countQuery, null)

        val count = cursor.count
        cursor.close()

        return count
    }

    @Suppress("unused")
    fun deleteAlbum(album: Album) {
        val db = writableDatabase
        db.delete(Album.TABLE_NAME, Album.COLUMN_ID + " = ?", arrayOf(album.id.toString()))
        db.close()
    }
}