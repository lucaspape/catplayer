package de.lucaspape.flavor

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.StreamDatabaseHelper

fun playYoutubeLivestream(view: View, streamName: String) {
    val streamDatabaseHelper = StreamDatabaseHelper(view.context)
    
    val stream = streamDatabaseHelper.getStream(streamName)
    stream?.let {
        val context = view.context
        val alertDialogBuilder = MaterialAlertDialogBuilder(context)
        alertDialogBuilder.setTitle("Cannot play youtube livestream in app. Open in browser?")
        alertDialogBuilder.setPositiveButton(context.getString(R.string.yes)) { _, _ ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(stream.streamUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        alertDialogBuilder.setNegativeButton(context.getString(R.string.no)) { _, _ ->

        }

        val dialog = alertDialogBuilder.create()
        dialog.show()

        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

        val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.setTextColor(typedValue.data)

        val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        negativeButton.setTextColor(typedValue.data)
    }
}

//fake function
fun getYoutubeLivestreamUrl(context: Context, name:String, callback:(streamUrl:String)->Unit){
    
}