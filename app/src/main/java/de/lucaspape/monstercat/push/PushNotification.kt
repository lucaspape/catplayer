package de.lucaspape.monstercat.push

import android.content.Context
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging

fun subscribeToChannel(context:Context, channelName:String){
    FirebaseMessaging.getInstance().subscribeToTopic(channelName)
        .addOnCompleteListener { task ->
            var msg = "Subscribed"
            if (!task.isSuccessful) {
                msg = "Failed"
            }

            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
}

fun unsubscribeToChannel(context:Context, channelName:String){
    FirebaseMessaging.getInstance().unsubscribeFromTopic(channelName)
        .addOnCompleteListener { task ->
            var msg = "Unsubscribed"
            if (!task.isSuccessful) {
                msg = "Failed"
            }

            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
}

