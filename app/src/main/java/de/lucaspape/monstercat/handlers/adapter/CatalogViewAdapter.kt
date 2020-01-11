package de.lucaspape.monstercat.handlers.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SimpleAdapter
import de.lucaspape.monstercat.R

private val from = arrayOf("shownTitle", "artist", "secondaryImage", "downloadedCheck")
private val to = arrayOf(R.id.title, R.id.artist, R.id.cover, R.id.titleDownloadStatus)

class CatalogViewAdapter(
    context:Context,
    data: ArrayList<HashMap<String, Any?>>,
    private val showContextMenu: (position: Int) -> Unit
) :
    SimpleAdapter(context, data, R.layout.list_single, from, to.toIntArray()) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val v = super.getView(position, convertView, parent)

        val button = v.findViewById<ImageButton>(R.id.titleMenuButton)

        button.setOnClickListener {
            showContextMenu(position)
        }

        return v
    }
}