package de.lucaspape.monstercat.core.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.core.database.objects.Album
import java.lang.IndexOutOfBoundsException

class AlbumDatabaseHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {
    companion object {
        @JvmStatic
        val DATABASE_VERSION = 1001

        @JvmStatic
        private val DATABASE_NAME = "albums_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS " + Album.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(Album.CREATE_TABLE)
    }

    fun reCreateTable(context: Context, reCreateItems: Boolean) {
        if (reCreateItems) {
            val albums = getAllAlbums()

            for (album in albums) {
                val albumItemDatabaseHelper = ItemDatabaseHelper(context, album.albumId)
                albumItemDatabaseHelper.reCreateTable()
            }
        }

        val db = writableDatabase

        db?.execSQL("DROP TABLE IF EXISTS " + Album.TABLE_NAME)
        onCreate(db)
    }

    fun insertAlbum(
        albumId: String,
        title: String,
        artist: String,
        version: String,
        mcID: String
    ): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(Album.COLUMN_TITLE, title)
        values.put(Album.COLUMN_ALBUM_ID, albumId)
        values.put(Album.COLUMN_ARTIST, artist)
        values.put(Album.COLUMN_VERSION, version)
        values.put(Album.COLUMN_ALBUM_MCID, mcID)

        val id = db.insert(Album.TABLE_NAME, null, values)
        db.close()

        return id
    }

    fun getAlbumFromMcId(mcID: String): Album? {
        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                Album.TABLE_NAME, arrayOf(
                    Album.COLUMN_ID,
                    Album.COLUMN_ALBUM_ID,
                    Album.COLUMN_TITLE,
                    Album.COLUMN_ARTIST,
                    Album.COLUMN_VERSION,
                    Album.COLUMN_ALBUM_MCID
                ),
                Album.COLUMN_ALBUM_MCID + "=?",
                arrayOf(mcID), null, null, null, null
            )

            cursor?.moveToFirst()

            try {
                val album = Album(
                    cursor.getInt(cursor.getColumnIndex(Album.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ALBUM_ID)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ARTIST)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_VERSION)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ALBUM_MCID))
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
                    Album.COLUMN_VERSION,
                    Album.COLUMN_ALBUM_MCID
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
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_VERSION)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ALBUM_MCID))
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

    fun getAlbums(skip: Long, limit: Long): List<Album> {
        val albums: ArrayList<Album> = ArrayList()

        val selectQuery = "SELECT * FROM " + Album.TABLE_NAME + " ORDER BY " +
                Album.COLUMN_ID + " ASC LIMIT $skip,$limit"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val album = Album(
                    cursor.getInt(cursor.getColumnIndex(Album.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ALBUM_ID)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ARTIST)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_VERSION)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ALBUM_MCID))
                )
                albums.add(album)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return albums
    }

    private fun getAllAlbums(): List<Album> {
        val albums: ArrayList<Album> = ArrayList()

        val selectQuery = "SELECT * FROM " + Album.TABLE_NAME + " ORDER BY " +
                Album.COLUMN_ID + " ASC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val album = Album(
                    cursor.getInt(cursor.getColumnIndex(Album.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ALBUM_ID)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ARTIST)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_VERSION)),
                    cursor.getString(cursor.getColumnIndex(Album.COLUMN_ALBUM_MCID))
                )
                albums.add(album)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return albums
    }

}