package de.lucaspape.monstercat.core.database.objects

data class CatalogSong(val id: Int, val songId: String) {
    companion object {
        @JvmStatic
        val TABLE_NAME = "catalog_song"

        @JvmStatic
        val COLUMN_ID = "id"

        @JvmStatic
        val COLUMN_SONG_ID = "songId"

        @JvmStatic
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_SONG_ID + " TEXT" +
                    ")"
    }

}