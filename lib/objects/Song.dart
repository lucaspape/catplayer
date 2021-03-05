class Song {
  static const String TABLE_NAME = "song";
  static const String COLUMN_SONG_ID = "songId";
  static const String COLUMN_TITLE = "title";
  static const String COLUMN_VERSION = "version";
  static const String COLUMN_ALBUM_ID = "albumId";
  static const String COLUMN_ALBUM_MC_ID = "mcALbumId";
  static const String COLUMN_ARTIST = "artist";
  static const String COLUMN_ARTIST_ID = "artistId";
  static const String COLUMN_DOWNLOADABLE = "downloadable";
  static const String COLUMN_STREAMABLE = "streamable";
  static const String COLUMN_IN_EARLY_ACCESS = "inEarlyAccess";
  static const String COLUMN_CREATOR_FRIENDLY = "creatorFriendly";
  static const String CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " (" +
          COLUMN_SONG_ID + " TEXT PRIMARY KEY," +
          COLUMN_TITLE + " TEXT," +
          COLUMN_VERSION + " TEXT," +
          COLUMN_ALBUM_ID + " TEXT," +
          COLUMN_ALBUM_MC_ID + " TEXT," +
          COLUMN_ARTIST + " TEXT," +
          COLUMN_ARTIST_ID + " TEXT," +
          COLUMN_DOWNLOADABLE + " TEXT," +
          COLUMN_STREAMABLE + " TEXT," +
          COLUMN_IN_EARLY_ACCESS + " TEXT," +
          COLUMN_CREATOR_FRIENDLY + " TEXT" +
          ")";

  Map<String, String> toMap() {
    return <String, String>{
      COLUMN_SONG_ID: songId,
      COLUMN_TITLE: title,
      COLUMN_VERSION: version,
      COLUMN_ALBUM_ID: albumId,
      COLUMN_ALBUM_MC_ID: mcAlbumId,
      COLUMN_ARTIST: artist,
      COLUMN_ARTIST_ID: artistId,
      COLUMN_DOWNLOADABLE: isDownloadable.toString(),
      COLUMN_STREAMABLE: isStreamable.toString(),
      COLUMN_IN_EARLY_ACCESS: inEarlyAccess.toString(),
      COLUMN_CREATOR_FRIENDLY: isCreatorFriendly.toString()
    };
  }

  Song.fromMap(Map<String, String> map) {
    songId = map[COLUMN_SONG_ID];
    title = map[COLUMN_TITLE];
    version = map[COLUMN_VERSION];
    albumId = map[COLUMN_ALBUM_ID];
    mcAlbumId = map[COLUMN_ALBUM_MC_ID];
    artist = map[COLUMN_ARTIST];
    artistId = map[COLUMN_ARTIST_ID];
    isDownloadable = map[COLUMN_DOWNLOADABLE].toLowerCase() == 'true';
    isStreamable = map[COLUMN_STREAMABLE].toLowerCase() == 'true';
    inEarlyAccess = map[COLUMN_IN_EARLY_ACCESS].toLowerCase() == 'true';
    isCreatorFriendly = map[COLUMN_CREATOR_FRIENDLY].toLowerCase() == 'true';
  }

  String songId;
  String title;
  String version;
  String albumId;
  String mcAlbumId;
  String artist;
  String artistId;
  bool isDownloadable;
  bool isStreamable;
  bool inEarlyAccess;
  bool isCreatorFriendly;

  Song(
      {this.songId,
        this.title,
        this.version,
        this.albumId,
        this.mcAlbumId,
        this.artist,
        this.artistId,
        this.isDownloadable,
        this.isStreamable,
        this.inEarlyAccess,
        this.isCreatorFriendly});
}