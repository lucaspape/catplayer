package de.lucaspape.monstercat.core.database.objects

class Item(val databaseId: String, val id: Long, val songId: String) {
    val tableName = "\"" + databaseId + "_item\""
    val createTable =
        "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
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