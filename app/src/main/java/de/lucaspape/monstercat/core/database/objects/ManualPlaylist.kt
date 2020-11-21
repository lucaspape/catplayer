package de.lucaspape.monstercat.core.database.objects

data class ManualPlaylist(
    val id: Int,
    val playlistId: String
) {
    companion object {
        @JvmStatic
        val TABLE_NAME = "manualPlaylist"

        @JvmStatic
        val COLUMN_ID = "id"

        @JvmStatic
        val COLUMN_PLAYLIST_ID = "playlistId"

        @JvmStatic
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_PLAYLIST_ID + " TEXT" +
                    ")"
    }

}