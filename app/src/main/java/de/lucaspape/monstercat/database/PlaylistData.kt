package de.lucaspape.monstercat.database

class PlaylistData(var playlistID:String){
    companion object{
        @JvmStatic val COLUMN_ID = "id"
        @JvmStatic val COLUMN_SONG_ID = "songid"
    }

    var TABLE_NAME = "Playlistdata_" + playlistID
    var CREATE_TABLE =
        "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SONG_ID + " TEXT" +
                ")"

    var id:Int = 0
    var songId:String = ""

    constructor(playlistID: String, id:Int, songId:String): this(playlistID){
        this.id = id
        this.songId = songId

        TABLE_NAME = "Playlistdata_" + playlistID

        CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_SONG_ID + " TEXT" +
                    ")"
    }
}