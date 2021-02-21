package de.lucaspape.monstercat.core.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.core.database.objects.Stream
import java.lang.IndexOutOfBoundsException

class StreamDatabaseHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {
    companion object {
        @JvmStatic
        val DATABASE_VERSION = 1000

        @JvmStatic
        private val DATABASE_NAME = "streams_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS " + Stream.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(Stream.CREATE_TABLE)
    }

    fun reCreateTable() {
        val db = writableDatabase

        db?.execSQL("DROP TABLE IF EXISTS " + Stream.TABLE_NAME)
        onCreate(db)
    }

    fun insertStream(streamUrl:String, coverUrl:String, name:String): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(Stream.COLUMN_STREAM_URL, streamUrl)
        values.put(Stream.COLUMN_COVER_URL, coverUrl)
        values.put(Stream.COLUMN_NAME, name)

        val id = db.insert(Stream.TABLE_NAME, null, values)
        db.close()

        return id
    }

    fun getStream(name: String): Stream? {
        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                Stream.TABLE_NAME, arrayOf(
                    Stream.COLUMN_ID,
                    Stream.COLUMN_STREAM_URL,
                    Stream.COLUMN_COVER_URL,
                    Stream.COLUMN_NAME
                ),
                Stream.COLUMN_NAME + "=?",
                arrayOf(name), null, null, null, null
            )

            cursor?.moveToFirst()

            try {

                val stream = Stream(
                    cursor.getInt(cursor.getColumnIndex(Stream.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Stream.COLUMN_STREAM_URL)),
                    cursor.getString(cursor.getColumnIndex(Stream.COLUMN_COVER_URL)),
                    cursor.getString(cursor.getColumnIndex(Stream.COLUMN_NAME))
                )

                cursor.close()

                return stream
            } catch (e: IndexOutOfBoundsException) {
                cursor.close()
                db.close()
                return null
            }

        } catch (e: SQLiteException) {
            return null
        }
    }

    fun getAllStreams(): ArrayList<Stream> {
        val streams: ArrayList<Stream> = ArrayList()

        val selectQuery = "SELECT * FROM " + Stream.TABLE_NAME + " ORDER BY " +
                Stream.COLUMN_ID + " DESC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val stream = Stream(
                    cursor.getInt(cursor.getColumnIndex(Stream.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Stream.COLUMN_STREAM_URL)),
                    cursor.getString(cursor.getColumnIndex(Stream.COLUMN_COVER_URL)),
                    cursor.getString(cursor.getColumnIndex(Stream.COLUMN_NAME))
                )

                streams.add(stream)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return streams
    }
}