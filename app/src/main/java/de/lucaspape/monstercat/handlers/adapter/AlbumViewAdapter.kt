package de.lucaspape.monstercat.handlers.adapter

import android.content.Context
import android.widget.SimpleAdapter
import de.lucaspape.monstercat.R

private val from = arrayOf("title", "artist", "primaryImage")
private val to = arrayOf(R.id.albumTitle, R.id.albumArtist, R.id.cover)

class AlbumViewAdapter(
    context: Context,
    data: ArrayList<HashMap<String, Any?>>
) : SimpleAdapter(context, data, R.layout.list_album_view, from, to.toIntArray()) 