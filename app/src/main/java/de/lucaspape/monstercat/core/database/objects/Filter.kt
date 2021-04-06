package de.lucaspape.monstercat.core.database.objects

data class Filter(val id:Int, val filterType:String, val filter:String) {
    companion object {
        @JvmStatic
        val TABLE_NAME = "filter"

        @JvmStatic
        val COLUMN_ID = "id"

        @JvmStatic
        val COLUMN_FILTER_TYPE = "filterType"

        @JvmStatic
        val COLUMN_FILTER = "filter"

        @JvmStatic
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_FILTER_TYPE + " TEXT," +
                    COLUMN_FILTER + " TEXT" +
                    ")"
    }
}