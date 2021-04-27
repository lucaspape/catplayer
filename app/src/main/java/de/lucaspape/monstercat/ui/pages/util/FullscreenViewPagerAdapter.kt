package de.lucaspape.monstercat.ui.pages.util

import android.content.Context
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import androidx.viewpager.widget.PagerAdapter
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.music.util.*
import java.lang.IndexOutOfBoundsException
import java.util.*

class FullscreenViewPagerAdapter(private val context: Context) : PagerAdapter() {
    override fun getCount(): Int {
        return 3
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object` as ConstraintLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val item = layoutInflater.inflate(R.layout.fullscreen_image_item, container, false)
        val fullscreenImageView = item.findViewById<ImageView>(R.id.fullscreenAlbumImage)
        val fullscreenLyricsView = item.findViewById<TextView>(R.id.fullscreenLyrics)

        when (position) {
            0 -> {

            }

            1 -> {
                fullscreenImageView?.setImageBitmap(coverBitmap)

                coverBitmapChangedCallback = {
                    fullscreenImageView?.setImageBitmap(coverBitmap)
                }

                coverDrawableChangedCallback = {
                    fullscreenImageView?.setImageDrawable(coverDrawable)
                }

                setTagCallback = { target ->
                    fullscreenImageView?.tag = target
                }

                lyricsChangedCallback = {
                    fullscreenLyricsView.text = getSpannableLyricText(context)
                }

                lyricsChangedCallback()
            }
            2 -> {

            }
        }

        Objects.requireNonNull(container).addView(item)
        return item
    }

    private fun getSpannableLyricText(context: Context): Spanned {
        val previousLyric = try {
            lyricTextArray[currentLyricsIndex - 1].replace("\n", "<br><br>")
        } catch (e: IndexOutOfBoundsException) {
            ""
        }

        var currentLyric = try {
            lyricTextArray[currentLyricsIndex].replace("\n", "<br><br>")
        } catch (e: IndexOutOfBoundsException) {
            ""
        }

        val nextLyric = try {
            lyricTextArray[currentLyricsIndex + 1].replace("\n", "<br><br>")
        } catch (e: IndexOutOfBoundsException) {
            ""
        }

        if (previousLyric == "" && currentLyric == "" && nextLyric == "") {
            currentLyric = ""
        } else if ((currentLyric != "" && previousLyric != "") || (currentLyric != "" && nextLyric != "")) {
            currentLyric = "<h1>$currentLyric</h1>"
        }

        return if (currentLyric != "") {
            HtmlCompat.fromHtml(
                context.getString(R.string.lyrics) + ": <br> $previousLyric <br> $currentLyric <br> $nextLyric",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } else {
            HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        Objects.requireNonNull(container).removeView(`object` as View)
    }
}