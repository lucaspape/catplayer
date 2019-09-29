package de.lucaspape.monstercat.database

class Song() {
    companion object{
        @JvmStatic val TABLE_NAME = "Song"
        @JvmStatic val COLUMN_ID = "id"
        @JvmStatic val COLUMN_SONG_ID = "songid"
        @JvmStatic val COLUMN_TITLE = "title"
        @JvmStatic val COLUMN_VERSION = "version"
        @JvmStatic val COLUMN_ALBUM_ID = "albumid"
        @JvmStatic val COLUMN_ARTIST = "artist"
        @JvmStatic val COLUMN_COVER_URL = "coverurl"

        @JvmStatic val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_SONG_ID + " TEXT," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_VERSION + " TEXT," +
                    COLUMN_ALBUM_ID + " TEXT," +
                    COLUMN_ARTIST + " TEXT," +
                    COLUMN_COVER_URL + " TEXT" +
                    ")"
    }

    //stored in SQL
    var id: Int = 0
    var songId: String = ""
    var title: String = ""
    var version: String = ""
    var albumId = ""
    var artist: String = ""
    var coverUrl:String = ""

    var downloadLocation:String = ""
    var streamLocation:String = ""

    constructor(id: Int, songId:String, title: String, version:String, albumId:String, artist: String, coverUrl:String) : this(){
        this.id = id
        this.songId = songId
        this.title = title
        this.version = version
        this.albumId = albumId
        this.artist = artist
        this.coverUrl = coverUrl
    }

    fun getUrl():String{
        return if(downloadLocation != ""){
            downloadLocation
        }else{
            streamLocation
        }
    }
}