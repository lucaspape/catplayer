import 'package:monstercat/objects/Item.dart';
import 'package:sqflite/sqflite.dart';

class ItemDatabaseHelper {
  Database db;
  String databasePath = 'db-item.sql';
  
  String databaseId;

  ItemDatabaseHelper({this.databaseId});

  Future open() async {
    db = await openDatabase(databasePath, version: 1,
        onCreate: (Database db, int version) async {
          await db.execute(Item(databaseId:databaseId, id:0, songId:"").createTable);
        });
  }

  Future<int> insert(Item item) async {
    return await db.insert(item.tableName, item.toMap());
  }

  Future<List<Item>> getAll() async {
    List<Map> maps = await db.query(Item(databaseId:databaseId, id:0, songId:"").tableName,
        columns: [
          Item.COLUMN_SONG_ID,
          Item.COLUMN_SONG_ID
        ]);

    if (maps.length > 0) {
      List<Item> items = List();
      
      for(var map in maps){
        items.add(Item.fromMap(databaseId, map));
      }
      
      return items;
    } else {
      return null;
    }
  }

  Future close() async => db.close();
}