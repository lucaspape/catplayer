package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.ui.abstract_items.content.QueueItem
import de.lucaspape.monstercat.ui.abstract_items.util.HeaderTextItem
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import kotlin.collections.ArrayList

class QueueRecyclerPage : RecyclerViewPage() {
    override suspend fun idToAbstractItem(view: View, id: String): GenericItem {
        return if (id.contains("separator-")) {
            HeaderTextItem(id.replace("separator-", ""))
        } else {
            QueueItem(id.replace("item-", ""))
        }
    }

    private val lookupTable = HashMap<Int, String>()
    private val indexLookupTable = HashMap<String, Int>()

    override suspend fun onMenuButtonClick(
        view: View,
        viewData: ArrayList<GenericItem>,
        itemIndex: Int
    ) {
        val item = viewData[itemIndex]

        if (item is QueueItem) {
            when (lookupTable[itemIndex]) {
                "priority" -> {
                    indexLookupTable["priority-${item.songId}"]?.let {
                        if (prioritySongQueue[it] == item.songId) {
                            removeFromPriorityQueue(it)
                        }
                    }
                }
                "queue" -> {
                    indexLookupTable["queue-${item.songId}"]?.let {
                        if (songQueue[it] == item.songId) {
                            removeFromQueue(it)
                        }
                    }
                }
                "related" -> {
                    indexLookupTable["related-${item.songId}"]?.let {
                        if (relatedSongQueue[it] == item.songId) {
                            removeFromRelatedQueue(it)
                        }
                    }
                }
            }

            saveRecyclerViewPosition(view.context)
            onCreate(view)
        }
    }

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemIdList: ArrayList<String>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        if (skip == 0) {
            val content = ArrayList<String>()

            var indexInQueue = 0

            if (prioritySongQueue.size > 0) {
                content.add("separator-${context.getString(R.string.queue)}")

                for (songId in prioritySongQueue) {
                    lookupTable[content.size] = "priority"
                    content.add("item-$songId")
                    indexLookupTable["priority-$songId"] = indexInQueue
                    indexInQueue++
                }
            }

            indexInQueue = 0

            if (songQueue.size > 0) {
                content.add("separator-${context.getString(R.string.comingUp)}")

                for (songId in songQueue) {
                    lookupTable[content.size] = "queue"
                    content.add("item-$songId")
                    indexLookupTable["queue-$songId"] = indexInQueue
                    indexInQueue++
                }
            }

            indexInQueue = 0

            if (relatedSongQueue.size > 0) {
                content.add("separator-${context.getString(R.string.relatedSongsComingUp)}")

                for (songId in relatedSongQueue) {
                    lookupTable[content.size] = "related"
                    content.add("item-$songId")
                    indexLookupTable["related-$songId"] = indexInQueue
                    indexInQueue++
                }
            }

            callback(content)
        } else {
            callback(ArrayList())
        }
    }
}