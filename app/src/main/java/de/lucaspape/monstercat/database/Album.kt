package de.lucaspape.monstercat.database

class Album() {
    companion object{
        @JvmStatic val TABLE_NAME = "Album"
        @JvmStatic val COLUMN_ID = "id"
        @JvmStatic val COLUMN_ALBUM_ID = "albumid"
        @JvmStatic val COLUMN_TITLE = "title"
        @JvmStatic val COLUMN_ARTIST = "artist"
        @JvmStatic val COLUMN_COVER_URL = "coverurl"

        @JvmStatic val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_ALBUM_ID + " TEXT," +
                    COLUMN_ARTIST + " TEXT," +
                    COLUMN_COVER_URL + " TEXT" +
                    ")"
    }

    var id: Int = 0
    var title: String = ""
    var albumId = ""
    var artist: String = ""
    var coverUrl:String = ""

    constructor(id: Int, albumId:String, title: String, artist: String, coverUrl:String) : this(){
        this.id = id
        this.title = title
        this.albumId = albumId
        this.artist = artist
        this.coverUrl = coverUrl
    }
}