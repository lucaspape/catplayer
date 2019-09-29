package de.lucaspape.monstercat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.lang.IndexOutOfBoundsException

class PlaylistDataDatabaseHelper (context:Context, var playlistId:String) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){

    companion object{
        @JvmStatic private val DATABASE_VERSION = 1
        @JvmStatic private val DATABASE_NAME = "playlists_data_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val playlistData = PlaylistData(playlistId)

        db!!.execSQL("DROP TABLE IF EXISTS " + playlistData.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val playlistData = PlaylistData(playlistId)
        println(playlistData.CREATE_TABLE)
        db!!.execSQL(playlistData.CREATE_TABLE)
    }

    fun insertSongId(songId:String):Long{
        val db = writableDatabase

        val playlistData = PlaylistData(playlistId)
        val values = ContentValues()

        values.put(PlaylistData.COLUMN_SONG_ID, songId)

        val id = db.insert(playlistData.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getPlaylistData(id:Long):PlaylistData{
        val playlistData = PlaylistData(playlistId)
        val db = readableDatabase

        val cursor = db.query(playlistData.TABLE_NAME, arrayOf(
            PlaylistData.COLUMN_ID,
            PlaylistData.COLUMN_SONG_ID),
            Playlist.COLUMN_ID + "=?",
            arrayOf(id.toString()), null, null, null, null)

        cursor?.moveToFirst()

        val newPlaylistData = PlaylistData(playlistId, cursor.getInt(cursor.getColumnIndex(PlaylistData.COLUMN_ID)),
            cursor.getString(cursor.getColumnIndex(PlaylistData.COLUMN_SONG_ID)))

        cursor.close()

        return newPlaylistData
    }

    fun getPlaylistData(songId: String):PlaylistData?{
        val playlistData = PlaylistData(playlistId)

        val db = readableDatabase
        val cursor: Cursor

        try{
            cursor = db.query(playlistData.TABLE_NAME, arrayOf(
                PlaylistData.COLUMN_ID,
                PlaylistData.COLUMN_SONG_ID),
                Song.COLUMN_SONG_ID + "=?",
                arrayOf(playlistId), null, null, null, null)

            cursor?.moveToFirst()

            try {
                val newPlaylistData = PlaylistData(playlistId, cursor.getInt(cursor.getColumnIndex(Playlist.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(PlaylistData.COLUMN_SONG_ID)))

                cursor.close()

                return newPlaylistData
            }catch (e: IndexOutOfBoundsException){
                cursor.close()
                db.close()
                return null
            }

        }catch (e: SQLiteException){
            return null
        }
    }

    fun getAllData():List<PlaylistData>{
        val playlistData = PlaylistData(playlistId)
        val playlistDatas:ArrayList<PlaylistData> = ArrayList()

        val selectQuery = "SELECT * FROM " + playlistData.TABLE_NAME + " ORDER BY " +
                PlaylistData.COLUMN_ID + " DESC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if(cursor.moveToFirst()){
            do{
                val newPlaylistData = PlaylistData(playlistId)
                newPlaylistData.id = cursor.getInt(cursor.getColumnIndex(PlaylistData.COLUMN_ID))
                newPlaylistData.songId = cursor.getString(cursor.getColumnIndex(PlaylistData.COLUMN_SONG_ID))

                playlistDatas.add(newPlaylistData)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return playlistDatas
    }

    fun getPlaylistDataCount():Int{
        val playlistData = PlaylistData(playlistId)
        val countQuery = "SELECT * FROM " + playlistData.TABLE_NAME
        val db = readableDatabase
        val cursor = db.rawQuery(countQuery, null)

        val count = cursor.count
        cursor.close()

        return count
    }


    fun deletePlaylistData(dPlaylistData: PlaylistData){
        val playlistData = PlaylistData(playlistId)
        val db = writableDatabase
        db.delete(playlistData.TABLE_NAME, PlaylistData.COLUMN_ID + " = ?", arrayOf(dPlaylistData.id.toString()))
        db.close()
    }

}