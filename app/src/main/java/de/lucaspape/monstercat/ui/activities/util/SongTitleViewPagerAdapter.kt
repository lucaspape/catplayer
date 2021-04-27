package de.lucaspape.monstercat.ui.activities.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.PagerAdapter
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.music.nextSongId
import de.lucaspape.monstercat.core.music.previousSongId
import de.lucaspape.monstercat.core.music.util.artist
import de.lucaspape.monstercat.core.music.util.title
import java.util.*

class SongTitleViewPagerAdapter(
    private val context: Context,
    private val openFullscreenView: () -> Unit
) : PagerAdapter() {
    override fun getCount(): Int {
        return 3
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object` as ConstraintLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val item = layoutInflater.inflate(R.layout.currentsong_item, container, false)

        val songTextView = item.findViewById<TextView>(R.id.currentSongText)

        songTextView.setOnClickListener {
            openFullscreenView()
        }

        when (position) {
            0 -> {
                SongDatabaseHelper(context).getSong(previousSongId)?.let {
                    val titleWithArtist = "${it.shownTitle} - ${it.artist}"

                    songTextView.text = titleWithArtist
                }
            }
            1 -> {
                val titleWithArtist = "$title - $artist"

                songTextView.text = titleWithArtist
            }
            2 -> {
                SongDatabaseHelper(context).getSong(nextSongId)?.let {
                    val titleWithArtist = "${it.shownTitle} - ${it.artist}"

                    songTextView.text = titleWithArtist
                }
            }
        }

        Objects.requireNonNull(container).addView(item)

        return item
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        Objects.requireNonNull(container).removeView(`object` as View)
    }
}