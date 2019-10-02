package de.lucaspape.monstercat.database

class PlaylistData(){
    companion object{
        @JvmStatic val COLUMN_ID = "id"
        @JvmStatic val COLUMN_SONG_ID = "songid"
        @JvmStatic var TABLE_NAME = "Playlistdata"
        @JvmStatic var CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_SONG_ID + " TEXT" +
                    ")"
    }



    var id:Int = 0
    var songId:Long = 0

    constructor(id:Int, songId:Long): this(){
        this.id = id
        this.songId = songId
    }
}