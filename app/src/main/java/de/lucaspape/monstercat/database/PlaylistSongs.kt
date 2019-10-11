package de.lucaspape.monstercat.database

class PlaylistSongs() {
    companion object {
        @JvmStatic
        val COLUMN_ID = "id"
        @JvmStatic
        val COLUMN_SONG_ID = "songId"
        @JvmStatic
        var TABLE_NAME = "PlaylistSongs"
        @JvmStatic
        var CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_SONG_ID + " INTEGER" +
                    ")"
    }


    var id: Int = 0
    var songId: Long = 0

    constructor(id: Int, songId: Long) : this() {
        this.id = id
        this.songId = songId
    }
}