package de.lucaspape.monstercat.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.R.id
import android.provider.ContactsContract.CommonDataKinds.Note
import de.lucaspape.monstercat.music.Song


class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object{
        @JvmStatic private val DATABASE_VERSION = 1
        @JvmStatic private val DATABASE_NAME = "songs_db"
    }


    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS " + SongListView.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(SongListView.CREATE_TABLE)
    }

    fun insertSong(song:String, artist:String):Long{
        val db = writableDatabase

        val values = ContentValues()

        values.put(SongListView.COLUMN_SONG, song)
        values.put(SongListView.COLUMN_ARTIST, artist)

        val id = db.insert(SongListView.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getSong(id:Long):SongListView{
        val db = readableDatabase

        val cursor = db.query(SongListView.TABLE_NAME, arrayOf(SongListView.COLUMN_ID, SongListView.COLUMN_SONG, SongListView.COLUMN_ARTIST),
            SongListView.COLUMN_ID + "=?",
            arrayOf(id.toString()), null, null, null, null)

        cursor?.moveToFirst()

        val songListView = SongListView(cursor.getInt(cursor.getColumnIndex(SongListView.COLUMN_ID)),
            cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_SONG)),
            cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_ARTIST)))

        cursor.close()

        return songListView
    }

    fun getAllSongs():List<SongListView>{
        val songs:ArrayList<SongListView> = ArrayList()

        val selectQuery = "SELECT * FROM " + SongListView.TABLE_NAME + " ORDER BY " +
                SongListView.COLUMN_ID + " DESC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if(cursor.moveToFirst()){
            do{
                val songListView = SongListView()
                songListView.id = cursor.getInt(cursor.getColumnIndex(SongListView.COLUMN_ID))
                songListView.song = cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_SONG))
                songListView.artist = cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_ARTIST))

                songs.add(songListView)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return songs
    }

    fun getSongsCount():Int{
        val countQuery = "SELECT * FROM " + SongListView.TABLE_NAME
        val db = readableDatabase
        val cursor = db.rawQuery(countQuery, null)

        val count = cursor.count
        cursor.close()

        return count
    }


    fun deleteSong(songListView: SongListView){
        val db = writableDatabase
        db.delete(SongListView.TABLE_NAME, SongListView.COLUMN_ID + " = ?", arrayOf(songListView.id.toString()))
        db.close()
    }

}