package de.lucaspape.monstercat.music

import android.content.Context
import java.io.*

data class PlayerSaveState(
    val loop: Boolean,
    val loopSingle: Boolean,
    val shuffle: Boolean,
    val crossfade: Int,
    val playlist: ArrayList<String>,
    val playlistIndex: Int,
    val nextRandom: Int,
    val songQueue: ArrayList<String>,
    val prioritySongQueue: ArrayList<String>,
    val progress: Long?,
    val duration: Long?
) : Serializable {
    companion object {
        private const val serialVersionUID = 158352511676231

        @JvmStatic
        internal var restored = false
            private set

        @JvmStatic
        fun restoreMusicPlayerState(context: Context) {
            if (!restored) {
                try {
                    try {
                        val objectInputStream =
                            ObjectInputStream(FileInputStream(File(context.cacheDir.toString() + "/player_state.obj")))

                        try {
                            val playerSaveState = objectInputStream.readObject() as PlayerSaveState

                            loop = playerSaveState.loop
                            loopSingle = playerSaveState.loopSingle
                            shuffle = playerSaveState.shuffle
                            crossfade = playerSaveState.crossfade
                            playlist = playerSaveState.playlist
                            playlistIndex = playerSaveState.playlistIndex
                            nextRandom = playerSaveState.nextRandom
                            songQueue = playerSaveState.songQueue
                            prioritySongQueue = playerSaveState.prioritySongQueue

                            val song = getCurrentSong()

                            song?.let {
                                prepareSong(context, song)

                                playerSaveState.progress?.let { progress ->
                                    playSong(context, song,
                                        showNotification = false,
                                        requestAudioFocus = false,
                                        playWhenReady = false,
                                        progress = progress
                                    )

                                    seekBarReference?.get()?.progress = progress.toInt()
                                }

                                playerSaveState.duration?.let { duration ->
                                    seekBarReference?.get()?.max = duration.toInt()
                                }

                            }
                        } catch (e: TypeCastException) {
                            File((context.cacheDir.toString() + "/player_state.obj")).delete()
                        }
                    }catch (e: EOFException){

                    }
                } catch (e: FileNotFoundException) {

                }

                restored = true
            }
        }

        @JvmStatic
        fun saveMusicPlayerState(context: Context) {
            val objectOutputStream =
                ObjectOutputStream(FileOutputStream(File(context.cacheDir.toString() + "/player_state.obj")))

            val playerSaveState = PlayerSaveState(
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

            objectOutputStream.writeObject(playerSaveState)
            objectOutputStream.flush()
            objectOutputStream.close()
        }
    }
}