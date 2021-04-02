package de.lucaspape.monstercat.core.music.save

import android.content.Context
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.nextRandom
import de.lucaspape.monstercat.core.music.playlist
import de.lucaspape.monstercat.core.music.playlistIndex
import de.lucaspape.monstercat.core.music.util.currentPosition
import de.lucaspape.monstercat.core.music.util.duration
import de.lucaspape.monstercat.core.music.util.playSong
import java.io.*
import java.util.*
import kotlin.ConcurrentModificationException
import kotlin.collections.ArrayList

data class PlayerSaveState(
    val loop: Boolean,
    val loopSingle: Boolean,
    val shuffle: Boolean,
    val crossfade: Int,
    val playlist: ArrayList<String>,
    val playlistIndex: Int,
    val nextRandom: Int,
    val songQueue: ArrayList<String>,
    val prioritySongQueue: LinkedList<String>,
    val progress: Long?,
    val duration: Long?
) : Serializable {
    companion object {
        private const val serialVersionUID = 158352511676231

        @JvmStatic
        private var restored = false

        @JvmStatic
        fun restore(context: Context, force: Boolean) {
            if (!restored || force) {
                try {
                    try {
                        val objectInputStream =
                            ObjectInputStream(FileInputStream(File(context.cacheDir.toString() + "/player_state.obj")))

                        try {
                            try {
                                val playerSaveState =
                                    objectInputStream.readObject() as PlayerSaveState

                                loop = playerSaveState.loop
                                loopSingle = playerSaveState.loopSingle
                                shuffle = playerSaveState.shuffle
                                crossfade = playerSaveState.crossfade
                                playlist = playerSaveState.playlist
                                playlistIndex = playerSaveState.playlistIndex
                                nextRandom = playerSaveState.nextRandom
                                songQueue = playerSaveState.songQueue
                                prioritySongQueue = playerSaveState.prioritySongQueue

                                playerSaveState.progress?.let { progress ->
                                    currentPosition = progress.toInt()

                                    playSong(
                                        context, currentSongId,
                                        showNotification = false,
                                        playWhenReady = false,
                                        progress = progress
                                    )
                                }

                                playerSaveState.duration?.let { sDuration ->
                                    duration = sDuration.toInt()
                                }
                            } catch (e: ClassNotFoundException) {
                                delete(context)
                            }

                        } catch (e: TypeCastException) {
                            delete(context)
                        }
                    } catch (e: EOFException) {

                    }
                } catch (e: FileNotFoundException) {

                }

                restored = true
            }
        }

        @JvmStatic
        fun save(context: Context) {
            val objectOutputStream =
                ObjectOutputStream(FileOutputStream(File(context.cacheDir.toString() + "/player_state.obj")))

            val playerSaveState =
                PlayerSaveState(
                    loop,
                    loopSingle,
                    shuffle,
                    crossfade,
                    playlist,
                    playlistIndex,
                    nextRandom,
                    songQueue,
                    prioritySongQueue,
                    exoPlayer?.currentPosition,
                    exoPlayer?.duration
                )

            try {
                objectOutputStream.writeObject(playerSaveState)
                objectOutputStream.flush()
                objectOutputStream.close()
            } catch (e: ConcurrentModificationException) {

            }
        }

        @JvmStatic
        fun delete(context: Context){
            try {
                File("${context.cacheDir}/player_state.obj").delete()
            } catch (e: FileNotFoundException) {

            }
        }
    }
}