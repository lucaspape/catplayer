package de.lucaspape.monstercat.push

import android.content.Context
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging

fun subscribeToChannel(context:Context, channelName:String){
    FirebaseMessaging.getInstance().subscribeToTopic(channelName)
        .addOnCompleteListener { task ->
            var msg = "Subscribed to $channelName"
            if (!task.isSuccessful) {
                msg = "Failed to subscribe to $channelName"
            }

            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
}

fun unsubscribeFromChannel(context:Context, channelName:String){
    FirebaseMessaging.getInstance().unsubscribeFromTopic(channelName)
        .addOnCompleteListener { task ->
            var msg = "Unsubscribed fom $channelName"
            if (!task.isSuccessful) {
                msg = "Failed to unsubscribe from $channelName"
            }

            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
}

