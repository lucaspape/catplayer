package de.lucaspape.monstercat.core.database.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.lucaspape.monstercat.core.database.objects.Song
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference

class SongDatabaseHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {

    companion object {
        @JvmStatic
        val DATABASE_VERSION = 7

        @JvmStatic
        private val DATABASE_NAME = "songs_db"

        @JvmStatic
        private var songDatabaseCache = HashMap<String, WeakReference<Song?>?>()
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        songDatabaseCache = HashMap()

        db?.execSQL("DROP TABLE IF EXISTS " + Song.TABLE_NAME)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(Song.CREATE_TABLE)
    }

    fun insertSong(
        context: Context,
        songId: String,
        title: String,
        version: String,
        albumId: String,
        mcAlbumId: String,
        artist: String,
        artistId: String,
        coverUrl: String,
        downloadable: Boolean,
        streamable: Boolean,
        inEarlyAccess: Boolean,
        creatorFriendly: Boolean
    ): String {
        val values = ContentValues()

        values.put(Song.COLUMN_SONG_ID, songId)
        values.put(Song.COLUMN_TITLE, title)
        values.put(Song.COLUMN_VERSION, version)
        values.put(Song.COLUMN_ALBUM_ID, albumId)
        values.put(Song.COLUMN_ALBUM_MC_ID, mcAlbumId)
        values.put(Song.COLUMN_ARTIST, artist)
        values.put(Song.COLUMN_ARTIST_ID, artistId)
        values.put(Song.COLUMN_COVER_URL, coverUrl)
        values.put(Song.COLUMN_DOWNLOADABLE, downloadable.toString())
        values.put(Song.COLUMN_STREAMABLE, streamable.toString())
        values.put(Song.COLUMN_INEARLYACCESS, inEarlyAccess.toString())
        values.put(Song.COLUMN_CREATOR_FRIENDLY, creatorFriendly.toString())

        if (getSong(context, songId) == null) {
            val db = writableDatabase
            db.insert(Song.TABLE_NAME, null, values)

            db.close()
        } else {
            val db = writableDatabase
            db.update(
                Song.TABLE_NAME,
                values,
                Song.COLUMN_SONG_ID + "=\'" + songId + "\'",
                null
            )

            db.close()
        }

        songDatabaseCache[songId] = null

        return songId
    }

    fun getSong(context: Context, songId: String): Song? {
        if(songDatabaseCache[songId]?.get() == null){
            val db = readableDatabase
            val cursor: Cursor

            try {
                cursor = db.query(
                    Song.TABLE_NAME, arrayOf(
                        Song.COLUMN_SONG_ID,
                        Song.COLUMN_TITLE,
                        Song.COLUMN_VERSION,
                        Song.COLUMN_ALBUM_ID,
                        Song.COLUMN_ALBUM_MC_ID,
                        Song.COLUMN_ARTIST,
                        Song.COLUMN_ARTIST_ID,
                        Song.COLUMN_COVER_URL,
                        Song.COLUMN_DOWNLOADABLE,
                        Song.COLUMN_STREAMABLE,
                        Song.COLUMN_INEARLYACCESS,
                        Song.COLUMN_CREATOR_FRIENDLY
                    ),
                    Song.COLUMN_SONG_ID + "=?",
                    arrayOf(songId), null, null, null, null
                )

                cursor?.moveToFirst()

                try {
                    val song = Song(
                        context,
                        cursor.getString(cursor.getColumnIndex(Song.COLUMN_SONG_ID)),
                        cursor.getString(cursor.getColumnIndex(Song.COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndex(Song.COLUMN_VERSION)),
                        cursor.getString(cursor.getColumnIndex(Song.COLUMN_ALBUM_ID)),
                        cursor.getString(cursor.getColumnIndex(Song.COLUMN_ALBUM_MC_ID)),
                        cursor.getString(cursor.getColumnIndex(Song.COLUMN_ARTIST)),
                        cursor.getString(cursor.getColumnIndex(Song.COLUMN_ARTIST_ID)),
                        cursor.getString(cursor.getColumnIndex(Song.COLUMN_COVER_URL)),
                        cursor.getString(cursor.getColumnIndex(Song.COLUMN_DOWNLOADABLE))!!
                            .toBoolean(),
                        cursor.getString(cursor.getColumnIndex(Song.COLUMN_STREAMABLE))!!
                            .toBoolean(),
                        cursor.getString(cursor.getColumnIndex(Song.COLUMN_INEARLYACCESS))!!
                            .toBoolean(),
                        cursor.getString(cursor.getColumnIndex(Song.COLUMN_CREATOR_FRIENDLY))!!
                            .toBoolean()
                    )

                    cursor.close()

                    songDatabaseCache[songId] = WeakReference(song)

                    return song
                } catch (e: IndexOutOfBoundsException) {
                    cursor.close()
                    db.close()
                    return null
                }

            } catch (e: SQLiteException) {
                return null
            }
        }else{
            return songDatabaseCache[songId]?.get()
        }
    }
}