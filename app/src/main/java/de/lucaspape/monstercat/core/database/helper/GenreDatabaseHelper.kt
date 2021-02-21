package de.lucaspape.monstercat.core.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.core.database.objects.Genre
import java.lang.IndexOutOfBoundsException

class GenreDatabaseHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {
    companion object {
        @JvmStatic
        val DATABASE_VERSION = 1000

        @JvmStatic
        private val DATABASE_NAME = "genre_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS " + Genre.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(Genre.CREATE_TABLE)
    }

    fun reCreateTable() {
        val db = writableDatabase

        db?.execSQL("DROP TABLE IF EXISTS " + Genre.TABLE_NAME)
        onCreate(db)
    }

    fun insertGenre(genreId: String, name:String): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(Genre.COLUMN_GENRE_ID, genreId)
        values.put(Genre.COLUMN_NAME, name)

        val id = db.insert(Genre.TABLE_NAME, null, values)
        db.close()

        return id
    }

    fun getGenre(moodId: String): Genre? {
        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                Genre.TABLE_NAME, arrayOf(
                    Genre.COLUMN_ID,
                    Genre.COLUMN_GENRE_ID,
                    Genre.COLUMN_NAME
                ),
                Genre.COLUMN_GENRE_ID + "=?",
                arrayOf(moodId), null, null, null, null
            )

            cursor?.moveToFirst()

            try {

                val mood = Genre(
                    cursor.getInt(cursor.getColumnIndex(Genre.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Genre.COLUMN_GENRE_ID)),
                    cursor.getString(cursor.getColumnIndex(Genre.COLUMN_NAME))
                )

                cursor.close()

                return mood
            } catch (e: IndexOutOfBoundsException) {
                cursor.close()
                db.close()
                return null
            }

        } catch (e: SQLiteException) {
            return null
        }
    }

    fun getAllGenres(): ArrayList<Genre> {
        val moods: ArrayList<Genre> = ArrayList()

        val selectQuery = "SELECT * FROM " + Genre.TABLE_NAME + " ORDER BY " +
                Genre.COLUMN_ID + " DESC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val mood = Genre(
                    cursor.getInt(cursor.getColumnIndex(Genre.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Genre.COLUMN_GENRE_ID)),
                    cursor.getString(cursor.getColumnIndex(Genre.COLUMN_NAME))
                )

                moods.add(mood)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return moods
    }
}