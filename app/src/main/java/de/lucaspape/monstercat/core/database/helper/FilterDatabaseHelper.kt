package de.lucaspape.monstercat.core.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.core.database.objects.Filter

class FilterDatabaseHelper(context:Context):
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {
    companion object {
        @JvmStatic
        val DATABASE_VERSION = 1001

        @JvmStatic
        private val DATABASE_NAME = "genre_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS " + Filter.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(Filter.CREATE_TABLE)
    }

    fun insertFilter(
        filterType: String,
        filter: String
    ): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(Filter.COLUMN_FILTER_TYPE, filterType)
        values.put(Filter.COLUMN_FILTER, filter)

        val id = db.insert(Filter.TABLE_NAME, null, values)
        db.close()

        return id
    }

    fun getAllFilters(): List<Filter> {
        val filters: ArrayList<Filter> = ArrayList()

        val selectQuery = "SELECT * FROM " + Filter.TABLE_NAME + " ORDER BY " +
                Filter.COLUMN_ID + " ASC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val filter = Filter(
                    cursor.getInt(cursor.getColumnIndex(Filter.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Filter.COLUMN_FILTER_TYPE)),
                    cursor.getString(cursor.getColumnIndex(Filter.COLUMN_FILTER))
                )
                filters.add(filter)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return filters
    }

    fun getFilterFromId(id: Int): Filter? {
        val db = readableDatabase

        try {
            val cursor = db.query(
                Filter.TABLE_NAME, arrayOf(
                    Filter.COLUMN_ID,
                    Filter.COLUMN_FILTER_TYPE,
                    Filter.COLUMN_FILTER
                ),
                Filter.COLUMN_ID + "=?",
                arrayOf(id.toString()), null, null, null, null
            )

            cursor?.moveToFirst()

            return try {
                val databaseItem =
                    Filter(
                        cursor.getInt(cursor.getColumnIndex(Filter.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(Filter.COLUMN_FILTER_TYPE)),
                        cursor.getString(cursor.getColumnIndex(Filter.COLUMN_FILTER)),
                    )

                cursor.close()

                databaseItem
            } catch (e: IndexOutOfBoundsException) {
                cursor.close()
                db.close()
                null
            }
        } catch (e: SQLiteException) {
            return null
        }
    }

    fun removeFilter(filterId: Int) {
        val db = writableDatabase
        db.delete(
            Filter.TABLE_NAME,
            Filter.COLUMN_ID + "=?",
            arrayOf(filterId.toString())
        )
        db.close()
    }
}