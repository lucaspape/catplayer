package de.lucaspape.monstercat.music

class Song(private val songHashMap:HashMap<String, Any?>){
    fun getUrl():String{
        return if(songHashMap["songDownloadLocation"] != null){
            songHashMap["songDownloadLocation"] as String
        }else{
            songHashMap["songStreamLocation"] as String
        }
    }

    val title = songHashMap["title"] as String
    val artist = songHashMap["artist"] as String
    val coverLocation = songHashMap["coverLocation"] as String + songHashMap["primaryRes"] as String
}