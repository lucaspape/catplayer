import 'package:monstercat/objects/Song.dart';
import 'package:sqflite/sqflite.dart';

class SongDatabaseHelper  {
  Database db;
  String databasePath = 'db-song.sql';

  Future open() async {
    db = await openDatabase(databasePath, version: 1,
        onCreate: (Database db, int version) async {
          await db.execute(Song.CREATE_TABLE);
        });
  }

  Future<int> insert(Song song) async {
    return await db.insert(Song.TABLE_NAME, song.toMap());
  }

  Future<Song> get(String songId) async {
    List<Map> maps = await db.query(Song.TABLE_NAME,
        columns: [
          Song.COLUMN_SONG_ID,
          Song.COLUMN_TITLE,
          Song.COLUMN_VERSION,
          Song.COLUMN_ALBUM_ID,
          Song.COLUMN_ALBUM_MC_ID,
          Song.COLUMN_ARTIST,
          Song.COLUMN_ARTIST_ID,
          Song.COLUMN_DOWNLOADABLE,
          Song.COLUMN_STREAMABLE,
          Song.COLUMN_IN_EARLY_ACCESS,
          Song.COLUMN_CREATOR_FRIENDLY
        ],
        where: '${Song.COLUMN_SONG_ID} = ?',
        whereArgs: [songId]);

    if (maps.length > 0) {
      return Song.fromMap(maps.first);
    } else {
      return null;
    }
  }

  Future close() async => db.close();
}