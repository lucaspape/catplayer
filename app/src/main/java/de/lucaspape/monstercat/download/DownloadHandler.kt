package de.lucaspape.monstercat.download

//this lists contain the urls that should be downloaded
internal val downloadList = ArrayList<HashMap<String, Any?>?>()
internal val downloadCoverArrayListList = ArrayList<ArrayList<HashMap<String, Any?>>?>()

fun addDownloadSong(url: String, location: String, shownTitle: String) {
    val downloadTrack = HashMap<String, Any?>()
    downloadTrack["url"] = url
    downloadTrack["location"] = location
    downloadTrack["shownTitle"] = shownTitle

    downloadList.add(downloadTrack)
}