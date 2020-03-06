package de.lucaspape.monstercat.music

import java.io.Serializable

data class PlayerSaveState(
    val loop: Boolean,
    val loopSingle: Boolean,
    val shuffle: Boolean,
    val crossfade: Int,
    val playlist: ArrayList<String>,
    val playlistIndex: Int,
    val nextRandom: Int,
    val songQueue:ArrayList<String>,
    val prioritySongQueue:ArrayList<String>,
    val progress:Long?,
    val duration:Long?
):Serializable{
    companion object {
        private const val serialVersionUID = 158352511676231
    }
}