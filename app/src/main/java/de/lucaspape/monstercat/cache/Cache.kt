package de.lucaspape.monstercat.cache

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*

class Cache(private val name: String, private val context: Context) {

    private val cacheFile = File(context.cacheDir.toString() + "/" + name)
    private var cacheData: HashMap<String, String?> = HashMap()

    fun load(id: String): Any? {
        loadCache()

        val gson = Gson()
        val type = object : TypeToken<Any?>() {}.type

        return gson.fromJson(cacheData[id], type)
    }

    fun save(id: String, data: Any?) {
        val gson = Gson()
        val dataString = gson.toJson(data)
        println(dataString)
        cacheData[id] = dataString
        saveCache()
    }

    fun delete(){
        cacheFile.delete()
    }

    private fun loadCache() {
        val sharedPreferences = context.getSharedPreferences(name, 0)

        val storedHashMapString = sharedPreferences.getString(name, null)

        if(storedHashMapString != null){
            val gson = Gson()

            val type = object : TypeToken<HashMap<String, Any?>>() {}.type

            cacheData = gson.fromJson(storedHashMapString, type)
            println(cacheData)
        }else{
            println("it is null")
        }

    }

    private fun saveCache() {
        val sharedPreferences = context.getSharedPreferences(name, 0)

        val gson = Gson()
        val hashMapString = gson.toJson(cacheData)

        sharedPreferences.edit().putString(name, hashMapString).apply()
    }
}