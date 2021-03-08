package de.lucaspape.monstercat.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.abstract_items.alert_list.AlertListItem

fun displayInfo(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

fun displaySnackBar(
    view: View,
    msg: String,
    buttonText: String?,
    buttonListener: (view: View) -> Unit
) {
    try {
        val snackBar = Snackbar.make(view, msg, Snackbar.LENGTH_SHORT)

        buttonText?.let {
            snackBar.setAction(buttonText, buttonListener)
        }

        snackBar.show()
    } catch (e: IllegalArgumentException) {

    }
}

private var alertDialog: AlertDialog? = null

fun displayAlertDialogList(
    context: Context,
    headerItem: GenericItem?,
    listItems: ArrayList<AlertListItem>,
    onItemClick: (position: Int, item: AlertListItem) -> Unit
) {
    val alertDialogBuilder = MaterialAlertDialogBuilder(context, R.style.DialogSlideAnim)

    val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val alertListLayout = layoutInflater.inflate(R.layout.alert_list, null, false)

    alertDialogBuilder.setView(alertListLayout)
    alertDialogBuilder.setCancelable(true)

    alertDialog?.cancel()

    alertDialog = alertDialogBuilder.create()

    alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    alertDialog?.window?.setGravity(Gravity.BOTTOM)

    alertDialog?.show()

    val recyclerView = alertListLayout.findViewById<RecyclerView>(R.id.alertDialogList)
    recyclerView.layoutManager =
        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

    val headerAdapter = ItemAdapter<GenericItem>()
    val itemAdapter = ItemAdapter<AlertListItem>()
    val fastAdapter: FastAdapter<GenericItem> =
        FastAdapter.with(listOf(headerAdapter, itemAdapter))

    var indexOffset = 0

    headerItem?.let {
        headerAdapter.add(headerItem)
        indexOffset = -1
    }

    for (listItem in listItems) {
        itemAdapter.add(listItem)
    }

    recyclerView.adapter = fastAdapter

    fastAdapter.onClickListener = { _, _, _, position ->
        val index = position + indexOffset

        if (index >= 0) {
            onItemClick(index, listItems[index])
            alertDialog?.cancel()
        }

        false
    }
}

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}
