package de.lucaspape.monstercat.core.database.objects

data class Playlist(
    val id: Int,
    val playlistId: String,
    val playlistName: String,
    val ownPlaylist: Boolean,
    val public: Boolean
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
        val COLUMN_OWN_PLAYLIST = "ownPlaylist"

        @JvmStatic
        val COLUMN_PUBLIC = "public"

        @JvmStatic
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_PLAYLIST_ID + " TEXT," +
                    COLUMN_NAME + " TEXT," +
                    COLUMN_OWN_PLAYLIST + " TEXT," +
                    COLUMN_PUBLIC + " TEXT" +
                    ")"
    }

}