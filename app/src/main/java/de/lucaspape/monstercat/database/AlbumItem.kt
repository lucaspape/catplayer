package de.lucaspape.monstercat.database

data class AlbumItem(val albumId: String, val id: Long, val songId: Long) {
    val TABLE_NAME = "\"" + albumId + "_item\""
    val CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_SONG_ID + " INTEGER" +
                ")"

    companion object {
        @JvmStatic
        val COLUMN_ID = "id"
        @JvmStatic
        val COLUMN_SONG_ID = "song_id"
    }
}