package de.lucaspape.monstercat.music

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.twitch.Stream
import java.lang.ref.WeakReference

class UpdateLiveInfoAsync(private val contextReference: WeakReference<Context>,private val stream: Stream) : AsyncTask<Void, Void, String>(){
    override fun doInBackground(vararg params: Void?): String {
        while(true){
            contextReference.get()?.let {context ->
                stream.updateInfo(context) {
                    if(it.title != stream.title || it.artist != stream.artist){
                        setTitle(it.title, "", it.artist)

                        startTextAnimation()

                        createSongNotification(
                            it.title,
                            "",
                            it.artist,
                            ""
                        )
                    }
                }
            }

            Thread.sleep(1000)
        }
    }

}