package de.lucaspape.monstercat.core.database.objects

class Mood(
    val id: Int,
    val moodId: String,
    val uri: String,
    val coverUrl:String,
    val name:String
) {
    companion object {
        @JvmStatic
        val TABLE_NAME = "mood"

        @JvmStatic
        val COLUMN_ID = "id"

        @JvmStatic
        val COLUMN_MOOD_ID = "moodId"

        @JvmStatic
        val COLUMN_MOOD_URI = "uri"

        @JvmStatic
        val COLUMN_COVER_URL = "coverUrl"

        @JvmStatic
        val COLUMN_NAME = "name"

        @JvmStatic
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_MOOD_ID + " TEXT," +
                    COLUMN_MOOD_URI + " TEXT," +
                    COLUMN_COVER_URL + " TEXT," +
                    COLUMN_NAME + " TEXT" +
                    ")"
    }
}