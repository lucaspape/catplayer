package de.lucaspape.monstercat.cache

import android.content.Context
import java.io.*

class Cache(name: String, context: Context) {

    private val cacheFile = File(context.cacheDir.toString() + "/" + name)
    private var cacheData: HashMap<String, Any?> = HashMap()

    fun load(id: String): Any? {
        loadCache()
        return cacheData[id]
    }

    fun save(id: String, data: Any?) {
        cacheData[id] = data
        saveCache()
    }

    fun delete(){
        cacheFile.delete()
    }

    private fun loadCache() {
        if (cacheFile.exists()) {
            val ois = ObjectInputStream(FileInputStream(cacheFile))
            cacheData = ois.readObject() as HashMap<String, Any?>
            ois.close()
        } else {
            saveCache()
        }
    }

    private fun saveCache() {
        val oos = ObjectOutputStream(FileOutputStream(cacheFile))
        oos.writeObject(cacheData)
        oos.flush()
        oos.close()
    }
}