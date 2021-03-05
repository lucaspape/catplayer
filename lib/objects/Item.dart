class Item {
  static const String COLUMN_ID = "id";
  static const COLUMN_SONG_ID = "song_id";

  String tableName;
  String createTable;
  
  String databaseId;
  int id;
  String songId;

  Item({this.databaseId, this.id, this.songId}) {
    tableName = "\"" + databaseId + "_item\"";
    createTable = "CREATE TABLE IF NOT EXISTS " +
        tableName +
        " (" +
        COLUMN_ID +
        " INTEGER PRIMARY KEY AUTOINCREMENT," +
        COLUMN_SONG_ID +
        " TEXT" +
        ")";
  }

  Map<String, String> toMap() {
    return <String, String>{
      COLUMN_SONG_ID: id.toString()
    };
  }

  Item.fromMap(String databaseId, Map<String, String> map) {
    this.databaseId = databaseId;
    songId = map[COLUMN_SONG_ID];
    id = int.parse(map[COLUMN_ID]);
  }
}