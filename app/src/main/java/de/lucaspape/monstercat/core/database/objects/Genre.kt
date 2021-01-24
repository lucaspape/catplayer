package de.lucaspape.monstercat.core.database.objects

data class Genre(
    val id: Int,
    val genreId: String,
    val name:String
) {
    companion object {
        @JvmStatic
        val TABLE_NAME = "genre"

        @JvmStatic
        val COLUMN_ID = "id"

        @JvmStatic
        val COLUMN_GENRE_ID = "genreId"

        @JvmStatic
        val COLUMN_NAME = "name"

        @JvmStatic
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_GENRE_ID + " TEXT," +
                    COLUMN_NAME + " TEXT" +
                    ")"
    }
}