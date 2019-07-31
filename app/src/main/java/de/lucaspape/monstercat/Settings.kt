package de.lucaspape.monstercat

import android.content.Context
import java.io.*

class Settings{
    fun getSetting(context: Context, key:String):String?{
        val settingsFile = File(context.filesDir.toString() + "/settings.map")
        if(settingsFile.exists()){
            val ois = ObjectInputStream(FileInputStream(settingsFile))
            val settingsMap = ois.readObject() as HashMap<String, String>
            ois.close()

            return settingsMap.get(key)
        }else{
            return null
        }

    }

    fun saveSetting(context: Context, key:String, setting:String){
        val settingsFile = File(context.filesDir.toString() + "/settings.map")
        var settingsMap = HashMap<String, String>()

        if(settingsFile.exists()){
            val ois = ObjectInputStream(FileInputStream(settingsFile))
            settingsMap = ois.readObject() as HashMap<String, String>
            ois.close()
        }

        settingsMap.put(key, setting)

        val oos = ObjectOutputStream(FileOutputStream(settingsFile))
        oos.writeObject(settingsMap)
        oos.flush()
        oos.close()
    }
}