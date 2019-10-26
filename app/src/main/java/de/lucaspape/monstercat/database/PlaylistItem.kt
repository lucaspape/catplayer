package de.lucaspape.monstercat.database

data class PlaylistItem(val playlistId:String, val id:Long, val songId:Long){
    val TABLE_NAME = "\"" + playlistId + "_item\""
    val CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_SONG_ID + " INTEGER" +
                ")"

    companion object{
        @JvmStatic val COLUMN_ID = "id"
        @JvmStatic val COLUMN_SONG_ID = "song_id"
    }
}