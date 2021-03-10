package de.lucaspape.monstercat.core.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.core.database.objects.Item

class ItemDatabaseHelper(context: Context, private var databaseId: String) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {

    companion object {
        @JvmStatic
        private val DATABASE_VERSION = 1001

        @JvmStatic
        private val DATABASE_NAME = "album_items_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(
            "DROP TABLE IF EXISTS " + Item(
                databaseId,
                0,
                ""
            ).TABLE_NAME
        )
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase?) {
        db?.execSQL(
            Item(
                databaseId,
                0,
                ""
            ).CREATE_TABLE)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            Item(
                databaseId,
                0,
                ""
            ).CREATE_TABLE)
    }

    fun reCreateTable() {
        val db = writableDatabase

        db?.execSQL(
            "DROP TABLE IF EXISTS " + Item(
                databaseId,
                0,
                ""
            ).TABLE_NAME
        )
        onCreate(db)
    }

    fun insertSongId(songId: String): Long {
        val db = writableDatabase

        val values = ContentValues()

        values.put(Item.COLUMN_SONG_ID, songId)

        val id = db.insert(
            Item(
                databaseId,
                0,
                ""
            ).TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getItemFromSongId(songId: String): Item? {
        val db = readableDatabase

        try {
            val cursor = db.query(
                Item(
                    databaseId,
                    0,
                    ""
                ).TABLE_NAME, arrayOf(
                    Item.COLUMN_ID,
                    Item.COLUMN_SONG_ID
                ),
                Item.COLUMN_SONG_ID + "=?",
                arrayOf(songId), null, null, null, null
            )

            cursor?.moveToFirst()

            return try {
                val databaseItem =
                    Item(
                        databaseId,
                        cursor.getLong(cursor.getColumnIndex(Item.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(Item.COLUMN_SONG_ID))
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

    fun getAllData(ascending:Boolean): List<Item> {
        val sort = if(ascending){
            "ASC"
        }else{
            "DESC"
        }

        val databaseItems: ArrayList<Item> = ArrayList()

        val selectQuery = "SELECT * FROM " + Item(
            databaseId,
            0,
            ""
        ).TABLE_NAME + " ORDER BY " +
                Item.COLUMN_ID + " " + sort

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val newDatabaseItem =
                    Item(
                        databaseId,
                        cursor.getLong(cursor.getColumnIndex(Item.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(Item.COLUMN_SONG_ID))
                    )

                databaseItems.add(newDatabaseItem)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return databaseItems
    }

    fun getItems(skip: Long): ArrayList<Item> {
        val items: ArrayList<Item> = ArrayList()

        val selectQuery = "SELECT * FROM " + Item(
            databaseId,
            0,
            ""
        ).TABLE_NAME + " ORDER BY " +
                Item.COLUMN_ID + " ASC LIMIT $skip,-1"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val item =
                    Item(
                        databaseId,
                        cursor.getLong(cursor.getColumnIndex(Item.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(Item.COLUMN_SONG_ID))
                    )

                items.add(item)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return items
    }

    fun getItems(skip: Long, limit:Long): ArrayList<Item> {
        val items: ArrayList<Item> = ArrayList()

        val selectQuery = "SELECT * FROM " + Item(
            databaseId,
            0,
            ""
        ).TABLE_NAME + " ORDER BY " +
                Item.COLUMN_ID + " ASC LIMIT $skip,$limit"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val item =
                    Item(
                        databaseId,
                        cursor.getLong(cursor.getColumnIndex(Item.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(Item.COLUMN_SONG_ID))
                    )

                items.add(item)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return items
    }
}