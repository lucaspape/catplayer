package de.lucaspape.monstercat.database.objects

data class Playlist(
    val id: Int,
    val playlistId: String,
    val playlistName: String
) {
    companion object {
        @JvmStatic
        val TABLE_NAME = "playlist"

        @JvmStatic
        val COLUMN_ID = "id"

        @JvmStatic
        val COLUMN_PLAYLIST_ID = "playlistId"

        @JvmStatic
        val COLUMN_NAME = "name"

        @JvmStatic
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_PLAYLIST_ID + " TEXT," +
                    COLUMN_NAME + " TEXT" +
                    ")"
    }

}