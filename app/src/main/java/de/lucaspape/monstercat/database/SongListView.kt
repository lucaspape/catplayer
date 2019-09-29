package de.lucaspape.monstercat.database

class SongListView() {
    companion object{
        @JvmStatic val TABLE_NAME = "songlistview"
        @JvmStatic val COLUMN_ID = "id"
        @JvmStatic val COLUMN_SONG = "song"
        @JvmStatic val COLUMN_ARTIST = "artist"

        @JvmStatic val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_SONG + " TEXT," +
                    COLUMN_ARTIST + " TEXT" +
                    ")"
    }

    var id: Int = 0
    var song: String = ""
    var artist: String = ""

    constructor(id: Int, song: String, artist: String) : this(){
        this.id = id
        this.song = song
        this.artist = artist
    }
}