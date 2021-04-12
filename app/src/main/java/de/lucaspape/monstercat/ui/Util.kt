package de.lucaspape.monstercat.ui

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
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

    try {
        alertDialog?.cancel()
    } catch (e: IllegalArgumentException) {

    }

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

fun showInformation(view: View, information: String) {
    try {
        alertDialog?.cancel()
    } catch (e: IllegalArgumentException) {

    }

    alertDialog = MaterialAlertDialogBuilder(view.context).apply {
        setTitle(view.context.getString(R.string.information))
        setPositiveButton(view.context.getString(R.string.ok), null)
        setMessage(information)
    }.create()

    alertDialog?.run {
        show()

        val textColorTypedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorOnSurface, textColorTypedValue, true)

        val positiveButton = getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.setTextColor(textColorTypedValue.data)
    }
}

fun showConfirmationAlert(context: Context, title: String, onConfirmation: () -> Unit) {
    val alertDialogBuilder = MaterialAlertDialogBuilder(context)
    alertDialogBuilder.setTitle(title)
    alertDialogBuilder.setPositiveButton(context.getString(R.string.yes)) { _, _ ->
        onConfirmation()
    }

    alertDialogBuilder.setNegativeButton(context.getString(R.string.no)) { _, _ -> }

    val dialog = alertDialogBuilder.create()
    dialog.show()

    val typedValue = TypedValue()
    context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

    val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
    positiveButton.setTextColor(typedValue.data)

    val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
    negativeButton.setTextColor(typedValue.data)
}

fun showInputAlert(
    context: Context,
    cancelable: Boolean,
    layoutId: Int,
    editTextLayoutId: Int,
    rootView: ViewGroup?,
    title: String,
    startText: String?,
    hint: String?,
    onConfirmation: (text: String) -> Unit
) {
    MaterialAlertDialogBuilder(context).apply {
        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val inflatedLayout = if (rootView != null) {

            layoutInflater.inflate(
                layoutId,
                rootView,
                false
            )
        } else {
            layoutInflater.inflate(
                layoutId,
                null
            )
        }


        val editTextLayout =
            inflatedLayout.findViewById<EditText>(editTextLayoutId)

        startText?.let {
            editTextLayout.text = SpannableStringBuilder(it)
        }

        hint?.let {
            editTextLayout.hint = it
        }

        setTitle(title)

        setPositiveButton(context.getString(R.string.ok)) { _, _ ->
            onConfirmation(editTextLayout.text.toString())
        }

        setView(inflatedLayout)
        setCancelable(cancelable)
    }.create().run {
        show()

        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

        val positiveButton = getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.setTextColor(typedValue.data)

        val negativeButton = getButton(DialogInterface.BUTTON_NEGATIVE)
        negativeButton.setTextColor(typedValue.data)
    }
}