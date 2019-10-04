package de.lucaspape.monstercat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException

class PlaylistDatabaseHelper (context:Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){
    companion object{
        @JvmStatic private val DATABASE_VERSION = 1
        @JvmStatic private val DATABASE_NAME = "playlists_db"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS " + Playlist.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(Playlist.CREATE_TABLE)
    }

    fun insertPlaylist(playlistId:String, name: String, trackCount:Int):Long{
        val db = writableDatabase

        val values = ContentValues()

        values.put(Playlist.COLUMN_PLAYLIST_ID, playlistId)
        values.put(Playlist.COLUMN_NAME, name)
        values.put(Playlist.COLUMN_TRACK_COUNT, trackCount)

        val id = db.insert(Playlist.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getPlaylist(id:Long):Playlist{
        val db = readableDatabase

        val cursor = db.query(Playlist.TABLE_NAME, arrayOf(
            Playlist.COLUMN_ID,
            Playlist.COLUMN_PLAYLIST_ID,
            Playlist.COLUMN_NAME,
            Playlist.COLUMN_TRACK_COUNT),
            Playlist.COLUMN_ID + "=?",
            arrayOf(id.toString()), null, null, null, null)

        cursor?.moveToFirst()

        val playlist = Playlist(cursor.getInt(cursor.getColumnIndex(Playlist.COLUMN_ID)),
            cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_PLAYLIST_ID)),
            cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_NAME)),
            cursor.getInt(cursor.getColumnIndex(Playlist.COLUMN_TRACK_COUNT)))

        cursor.close()

        return playlist
    }

    fun getPlaylist(playlistId: String):Playlist?{
        val db = readableDatabase
        val cursor: Cursor

        try{
            cursor = db.query(Playlist.TABLE_NAME, arrayOf(
                Playlist.COLUMN_ID,
                Playlist.COLUMN_PLAYLIST_ID,
                Playlist.COLUMN_NAME,
                Playlist.COLUMN_TRACK_COUNT),
                Playlist.COLUMN_PLAYLIST_ID + "=?",
                arrayOf(playlistId), null, null, null, null)

            cursor?.moveToFirst()

            try {

                val playlist = Playlist(cursor.getInt(cursor.getColumnIndex(Playlist.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_PLAYLIST_ID)),
                    cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_NAME)),
                    cursor.getInt(cursor.getColumnIndex(Playlist.COLUMN_TRACK_COUNT)))

                cursor.close()

                return playlist
            }catch (e: IndexOutOfBoundsException){
                cursor.close()
                db.close()
                return null
            }

        }catch (e: SQLiteException){
            return null
        }
    }

    fun getAllPlaylists():List<Playlist>{
        val playlists:ArrayList<Playlist> = ArrayList()

        val selectQuery = "SELECT * FROM " + Playlist.TABLE_NAME + " ORDER BY " +
                Playlist.COLUMN_ID + " DESC"

        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if(cursor.moveToFirst()){
            do{
                val playlist = Playlist()
                playlist.id = cursor.getInt(cursor.getColumnIndex(Playlist.COLUMN_ID))
                playlist.playlistId = cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_PLAYLIST_ID))
                playlist.playlistName = cursor.getString(cursor.getColumnIndex(Playlist.COLUMN_NAME))
                playlist.trackCount = cursor.getInt(cursor.getColumnIndex(Playlist.COLUMN_TRACK_COUNT))

                playlists.add(playlist)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return playlists
    }

    fun getPlaylistsCount():Int{
        val countQuery = "SELECT * FROM " + Playlist.TABLE_NAME
        val db = readableDatabase
        val cursor = db.rawQuery(countQuery, null)

        val count = cursor.count
        cursor.close()

        return count
    }


    fun deletePlaylist(playlist: Playlist){
        val db = writableDatabase
        db.delete(Playlist.TABLE_NAME, Playlist.COLUMN_ID + " = ?", arrayOf(playlist.id.toString()))
        db.close()
    }
}