package de.lucaspape.monstercat.core.database.objects

data class Item(val databaseId: String, val id: Long, val songId: String) {
    val TABLE_NAME = "\"" + databaseId + "_item\""
    val CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_SONG_ID + " TEXT" +
                ")"

    companion object {
        @JvmStatic
        val COLUMN_ID = "id"

        @JvmStatic
        val COLUMN_SONG_ID = "song_id"
    }
}