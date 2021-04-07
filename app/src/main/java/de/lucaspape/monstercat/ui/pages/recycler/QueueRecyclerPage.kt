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
import de.lucaspape.monstercat.ui.pages.util.StringItem
import java.lang.IndexOutOfBoundsException
import kotlin.collections.ArrayList
import kotlin.random.Random

class QueueRecyclerPage : RecyclerViewPage() {
    override fun registerListeners(view: View) {
        playlistChangedCallback = {
            reload(view)
        }
    }

    override suspend fun itemToAbstractItem(view: View, item: Item): GenericItem? {
        return if(item is StringItem){
            if (item.typeId == "separator") {
                HeaderTextItem(item.itemId)
            } else {
                QueueItem(item.itemId)
            }
        }else{
            null
        }
    }

    private var lookupTable = HashMap<Int, String>()
    private var indexLookupTable = HashMap<String, Int>()

    override suspend fun onMenuButtonClick(
        view: View,
        viewData: List<GenericItem>,
        itemIndex: Int
    ) {
        try{
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

                removeItem(itemIndex) {
                    if (shuffle) {
                        saveRecyclerViewPosition(view.context)
                        reload(view)
                    } else {
                        generateLookupTable()
                    }
                }
            }
        }catch (e: IndexOutOfBoundsException){
            reload(view)
        }
    }

    private fun generateLookupTable(){
        lookupTable = HashMap()
        indexLookupTable = HashMap()

        var totalSize = 0

        var queue = ArrayList<String>()
        prioritySongQueue.forEach { queue.add(it) }

        if (queue.size > 0) {
            totalSize++

            for ((indexInQueue, songId) in queue.withIndex()) {
                lookupTable[totalSize] = "priority"
                totalSize++
                indexLookupTable["priority-$songId"] = indexInQueue
            }
        }

        queue = ArrayList()
        songQueue.forEach { queue.add(it) }

        if (queue.size > 0) {
            totalSize++

            if (!shuffle) {
                for ((indexInQueue, songId) in queue.withIndex()) {
                    lookupTable[totalSize] = "queue"
                    totalSize++
                    indexLookupTable["queue-$songId"] = indexInQueue
                }
            } else {
                var nextRandom = Random(randomSeed).nextInt(queue.size)

                while (queue.size > 0) {
                    val songId = queue[nextRandom]

                    lookupTable[totalSize] = "queue"
                    totalSize++

                    //not perfect bc double songs but better than nothin
                    indexLookupTable["queue-$songId"] = songQueue.indexOf(songId)

                    queue.removeAt(nextRandom)
                    if (queue.size > 0) {
                        nextRandom = Random(randomSeed).nextInt(queue.size)
                    }
                }
            }
        }

        queue = ArrayList()
        relatedSongQueue.forEach { queue.add(it) }

        if (queue.size > 0) {
            totalSize++

            if (!shuffle) {
                for ((indexInQueue, songId) in queue.withIndex()) {
                    lookupTable[totalSize] = "related"
                    totalSize++
                    indexLookupTable["related-$songId"] = indexInQueue
                }
            } else {
                var nextRandom = Random(relatedRandomSeed).nextInt(queue.size)

                while (queue.size > 0) {
                    val songId = queue[nextRandom]

                    lookupTable[totalSize] = "related"
                    totalSize++

                    //not perfect bc double songs but better than nothin
                    indexLookupTable["related-$songId"] = prioritySongQueue.indexOf(songId)

                    queue.removeAt(nextRandom)
                    if (queue.size > 0) {
                        nextRandom = Random(relatedRandomSeed).nextInt(queue.size)
                    }
                }
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

            var queue = ArrayList<String>()
            prioritySongQueue.forEach { queue.add(it) }

            if (queue.size > 0) {
                content.add(StringItem("separator", context.getString(R.string.queue)))

                for (songId in queue) {
                    content.add(StringItem("item", songId))
                }
            }

            queue = ArrayList()
            songQueue.forEach { queue.add(it) }

            if (queue.size > 0) {
                content.add(StringItem("separator", context.getString(R.string.comingUp)))

                if (!shuffle) {
                    for (songId in queue) {
                        content.add(StringItem("item", songId))
                    }
                } else {
                    var nextRandom = Random(randomSeed).nextInt(queue.size)

                    while (queue.size > 0) {
                        val songId = queue[nextRandom]

                        content.add(StringItem("item", songId))

                        queue.removeAt(nextRandom)
                        if (queue.size > 0) {
                            nextRandom = Random(randomSeed).nextInt(queue.size)
                        }
                    }
                }
            }

            queue = ArrayList()
            relatedSongQueue.forEach { queue.add(it) }

            if (queue.size > 0) {
                content.add(StringItem("separator", context.getString(R.string.relatedSongsComingUp)))

                if (!shuffle) {
                    for (songId in queue) {
                        content.add(StringItem("item", songId))
                    }
                } else {
                    var nextRandom = Random(relatedRandomSeed).nextInt(queue.size)

                    while (queue.size > 0) {
                        val songId = queue[nextRandom]

                        content.add(StringItem("item", songId))

                        queue.removeAt(nextRandom)
                        if (queue.size > 0) {
                            nextRandom = Random(relatedRandomSeed).nextInt(queue.size)
                        }
                    }
                }
            }

            generateLookupTable()

            callback(content)
        } else {
            callback(ArrayList())
        }
    }
}