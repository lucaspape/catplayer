package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.ui.abstract_items.content.QueueItem
import de.lucaspape.monstercat.ui.abstract_items.util.HeaderTextItem
import de.lucaspape.monstercat.ui.pages.util.Item
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import kotlin.collections.ArrayList
import kotlin.random.Random

class QueueRecyclerPage : RecyclerViewPage() {
    override suspend fun itemToAbstractItem(view: View, item: Item): GenericItem {
        return if (item.typeId == "separator") {
            HeaderTextItem(item.itemId)
        } else {
            QueueItem(item.itemId)
        }
    }

    private val lookupTable = HashMap<Int, String>()
    private val indexLookupTable = HashMap<String, Int>()

    override suspend fun onMenuButtonClick(
        view: View,
        viewData: List<GenericItem>,
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

            removeItem(itemIndex)

            if(shuffle){
                saveRecyclerViewPosition(view.context)
                onCreate(view)
            }
        }
    }

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemList: ArrayList<Item>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        if (skip == 0) {
            val content = ArrayList<Item>()

            if (prioritySongQueue.size > 0) {

                content.add(Item(context.getString(R.string.queue), "separator"))

                for ((indexInQueue, songId) in prioritySongQueue.withIndex()) {
                    lookupTable[content.size] = "priority"
                    content.add(Item(songId, "item"))
                    indexLookupTable["priority-$songId"] = indexInQueue
                }
            }

            if (songQueue.size > 0) {
                content.add(Item(context.getString(R.string.comingUp), "separator"))

                if (!shuffle) {
                    for ((indexInQueue, songId) in songQueue.withIndex()) {
                        lookupTable[content.size] = "queue"
                        content.add(Item(songId, "item"))
                        indexLookupTable["queue-$songId"] = indexInQueue
                    }
                } else {
                    val queue = ArrayList<String>()

                    songQueue.forEach { queue.add(it) }

                    var nextRandom = Random(randomSeed).nextInt(queue.size)

                    while (queue.size > 0) {
                        val songId = queue[nextRandom]

                        lookupTable[content.size] = "queue"
                        content.add(Item(songId, "item"))

                        //not perfect bc double songs but better than nothin
                        indexLookupTable["queue-$songId"] = songQueue.indexOf(songId)

                        queue.removeAt(nextRandom)
                        if (queue.size > 0) {
                            nextRandom = Random(randomSeed).nextInt(queue.size)
                        }
                    }
                }
            }

            if (relatedSongQueue.size > 0) {
                content.add(Item(context.getString(R.string.relatedSongsComingUp), "separator"))

                if (!shuffle) {
                    for ((indexInQueue, songId) in relatedSongQueue.withIndex()) {
                        lookupTable[content.size] = "related"
                        content.add(Item(songId, "item"))
                        indexLookupTable["related-$songId"] = indexInQueue
                    }
                } else {
                    val queue = ArrayList<String>()

                    prioritySongQueue.forEach { queue.add(it) }

                    var nextRandom = Random(relatedRandomSeed).nextInt(queue.size)

                    while (queue.size > 0) {
                        val songId = queue[nextRandom]

                        lookupTable[content.size] = "related"
                        content.add(Item(songId, "item"))

                        //not perfect bc double songs but better than nothin
                        indexLookupTable["related-$songId"] = prioritySongQueue.indexOf(songId)

                        queue.removeAt(nextRandom)
                        if (queue.size > 0) {
                            nextRandom = Random(relatedRandomSeed).nextInt(queue.size)
                        }
                    }
                }
            }

            callback(content)
        } else {
            callback(ArrayList())
        }
    }
}