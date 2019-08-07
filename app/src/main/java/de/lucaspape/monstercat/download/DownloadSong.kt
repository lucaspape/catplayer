package de.lucaspape.monstercat.download

import android.content.Context
import android.os.AsyncTask
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.target.Target
import de.lucaspape.monstercat.R
import java.io.*

class DownloadSong(
    private val url: String, private val location: String,
    private val sid: String, private val shownTitle: String,
    private val context: Context
) :
    AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg params: Void?): String? {
        try {

            val glideUrl = GlideUrl(
                url, LazyHeaders.Builder()
                    .addHeader("Cookie", "connect.sid=$sid").build()
            )


            try {
                val downloadFile = Glide.with(context)
                    .load(glideUrl)
                    .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get()

                val destFile = File(location)

                val bufferedInputStream = BufferedInputStream(FileInputStream(downloadFile))
                val bufferedOutputStream = BufferedOutputStream(FileOutputStream(destFile))

                val buffer = ByteArray(1024)

                var len: Int
                len = bufferedInputStream.read(buffer)
                while (len > 0) {
                    bufferedOutputStream.write(buffer, 0, len)
                    len = bufferedInputStream.read(buffer)
                }
                bufferedOutputStream.flush()
                bufferedOutputStream.close()
            } catch (e: GlideException) {
            }

        } catch (e: IOException) {
            // Log exception
            return null
        }

        return null
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        Toast.makeText(context, context.getString(R.string.downloadSuccessfulMsg, shownTitle), Toast.LENGTH_SHORT)
            .show()
    }
}