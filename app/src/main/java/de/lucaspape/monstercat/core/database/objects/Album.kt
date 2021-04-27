package de.lucaspape.monstercat.core.database.objects

class Album(
    val id: Int,
    val albumId: String,
    private val title: String,
    val artist: String,
    private val version: String,
    val mcID: String
) {
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
        val COLUMN_VERSION = "version"

        @JvmStatic
        val COLUMN_ALBUM_MCID = "albumMCID"

        @JvmStatic
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_ALBUM_ID + " TEXT," +
                    COLUMN_ARTIST + " TEXT," +
                    COLUMN_VERSION + " TEXT," +
                    COLUMN_ALBUM_MCID + " TEXT" +
                    ")"
    }

    val shownTitle:String
        get() {
            return if (version.isNotEmpty()){
                "$title ($version)"
            }else{
                title
            }
        }
}