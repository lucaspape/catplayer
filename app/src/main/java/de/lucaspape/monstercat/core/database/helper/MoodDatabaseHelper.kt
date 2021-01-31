package de.lucaspape.monstercat.core.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.core.database.objects.Mood
import java.lang.IndexOutOfBoundsException

class MoodDatabaseHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {
    companion object {
        @JvmStatic
        val DATABASE_VERSION = 4 * SongDatabaseHelper.DATABASE_VERSION

        @JvmStatic
        private val DATABASE_NAME = "mood_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS " + Mood.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(Mood.CREATE_TABLE)
    }

    fun reCreateTable() {
        val db = writableDatabase

        db?.execSQL("DROP TABLE IF EXISTS " + Mood.TABLE_NAME)
        onCreate(db)
    }

    fun insertMood(moodId: String, coverUrl:String, name:String): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(Mood.COLUMN_MOOD_ID, moodId)
        values.put(Mood.COLUMN_COVER_URL, coverUrl)
        values.put(Mood.COLUMN_NAME, name)

        val id = db.insert(Mood.TABLE_NAME, null, values)
        db.close()

        return id
    }

    fun getMood(moodId: String): Mood? {
        val db = readableDatabase
        val cursor: Cursor

        try {
            cursor = db.query(
                Mood.TABLE_NAME, arrayOf(
                    Mood.COLUMN_ID,
                    Mood.COLUMN_MOOD_ID,
                    Mood.COLUMN_COVER_URL,
                    Mood.COLUMN_NAME
                ),
                Mood.COLUMN_MOOD_ID + "=?",
                arrayOf(moodId), null, null, null, null
            )

            cursor?.moveToFirst()

            try {

                val mood = Mood(
                    cursor.getInt(cursor.getColumnIndex(Mood.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Mood.COLUMN_MOOD_ID)),
                    cursor.getString(cursor.getColumnIndex(Mood.COLUMN_COVER_URL)),
                    cursor.getString(cursor.getColumnIndex(Mood.COLUMN_NAME))
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

    fun getAllMoods(): ArrayList<Mood> {
        val moods: ArrayList<Mood> = ArrayList()

        val selectQuery = "SELECT * FROM " + Mood.TABLE_NAME + " ORDER BY " +
                Mood.COLUMN_ID + " DESC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val mood = Mood(
                    cursor.getInt(cursor.getColumnIndex(Mood.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Mood.COLUMN_MOOD_ID)),
                    cursor.getString(cursor.getColumnIndex(Mood.COLUMN_COVER_URL)),
                    cursor.getString(cursor.getColumnIndex(Mood.COLUMN_NAME))
                )

                moods.add(mood)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return moods
    }
}