package de.lucaspape.monstercat.database

data class Album(val id: Int,
                 val albumId: String,
                 val title: String,
                 val artist: String,
                 val coverUrl: String) {
    companion object {
        @JvmStatic
        val TABLE_NAME = "album"
        @JvmStatic
        val COLUMN_ID = "id"
        @JvmStatic
        val COLUMN_ALBUM_ID = "albumId"
        @JvmStatic
        val COLUMN_TITLE = "title"
        @JvmStatic
        val COLUMN_ARTIST = "artist"
        @JvmStatic
        val COLUMN_COVER_URL = "coverUrl"

        @JvmStatic
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_ALBUM_ID + " TEXT," +
                    COLUMN_ARTIST + " TEXT," +
                    COLUMN_COVER_URL + " TEXT" +
                    ")"
    }
}