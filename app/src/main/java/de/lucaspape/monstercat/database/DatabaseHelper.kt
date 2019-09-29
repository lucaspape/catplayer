package de.lucaspape.monstercat.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


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

    fun insertSong(songId:String, title: String, version:String, albumId:String, artist: String, coverUrl:String):Long{
        val db = writableDatabase

        val values = ContentValues()

        values.put(SongListView.COLUMN_SONG_ID, songId)
        values.put(SongListView.COLUMN_TITLE, title)
        values.put(SongListView.COLUMN_VERSION, version)
        values.put(SongListView.COLUMN_ALBUM_ID, albumId)
        values.put(SongListView.COLUMN_ARTIST, artist)
        values.put(SongListView.COLUMN_COVER_URL, coverUrl)

        val id = db.insert(SongListView.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getSong(id:Long):SongListView{
        val db = readableDatabase

        val cursor = db.query(SongListView.TABLE_NAME, arrayOf(
            SongListView.COLUMN_ID,
            SongListView.COLUMN_SONG_ID,
            SongListView.COLUMN_TITLE,
            SongListView.COLUMN_VERSION,
            SongListView.COLUMN_ALBUM_ID,
            SongListView.COLUMN_ARTIST,
            SongListView.COLUMN_COVER_URL),
            SongListView.COLUMN_ID + "=?",
            arrayOf(id.toString()), null, null, null, null)

        cursor?.moveToFirst()

        val songListView = SongListView(cursor.getInt(cursor.getColumnIndex(SongListView.COLUMN_ID)),
            cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_SONG_ID)),
            cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_TITLE)),
            cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_VERSION)),
            cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_ALBUM_ID)),
            cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_ARTIST)),
            cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_COVER_URL)))

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
                songListView.songId = cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_SONG_ID))
                songListView.title = cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_TITLE))
                songListView.version = cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_VERSION))
                songListView.albumId = cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_ALBUM_ID))
                songListView.artist = cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_ARTIST))
                songListView.coverUrl = cursor.getString(cursor.getColumnIndex(SongListView.COLUMN_COVER_URL))

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