package de.lucaspape.monstercat.database

class Playlist(){
    companion object{
        @JvmStatic val TABLE_NAME = "Playlist"
        @JvmStatic val COLUMN_ID = "id"
        @JvmStatic val COLUMN_PLAYLIST_ID = "playlistid"
        @JvmStatic val COLUMN_NAME = "name"
        @JvmStatic val COLUMN_TRACK_COUNT = "trackcount"

        @JvmStatic val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_PLAYLIST_ID + " TEXT," +
                    COLUMN_NAME + " TEXT," +
                    COLUMN_TRACK_COUNT + " INTEGER" +
                    ")"
    }

    var id:Int = 0
    var playlistId:String = ""
    var playlistName:String = ""
    var trackCount:Int = 0

    constructor(id:Int, playlistId:String, playlistName:String, trackCount:Int): this(){
        this.id = id
        this.playlistId = playlistId
        this.playlistName = playlistName
        this.trackCount = trackCount
    }
}