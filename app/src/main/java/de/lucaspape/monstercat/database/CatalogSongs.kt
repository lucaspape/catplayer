package de.lucaspape.monstercat.database

class CatalogSongs(){
    companion object{
        @JvmStatic val TABLE_NAME = "CatalogSongs"
        @JvmStatic val COLUMN_ID = "id"
        @JvmStatic val COLUMN_SONG_ID = "songid"

        @JvmStatic val CREATE_TABLE =
            "CREATE TABLE " + CatalogSongs.TABLE_NAME + " (" +
                    CatalogSongs.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    CatalogSongs.COLUMN_SONG_ID + " INTEGER" +
                    ")"
    }

    var id:Int = 0
    var songId:Long = 0

    constructor(id:Int, songId:Long): this(){
        this.id = id
        this.songId = songId
    }
}