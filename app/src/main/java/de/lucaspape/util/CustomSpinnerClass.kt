package de.lucaspape.util

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.widget.AppCompatSpinner

class CustomSpinnerClass : AppCompatSpinner,
    AdapterView.OnItemSelectedListener {
    private var mListener: OnItemSelectedListener? = null
    private var mUserActionOnSpinner = true

    constructor(context: Context) : super(context) {
        super.setOnItemSelectedListener(this)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        super.setOnItemSelectedListener(this)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        super.setOnItemSelectedListener(this)
    }

    override fun setSelection(position: Int, animate: Boolean) {
        val sameSelected = position == selectedItemPosition
        super.setSelection(position, animate)
        if (sameSelected) {
            //TODO:-> Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
            onItemSelectedListener?.onItemSelected(
                this,
                selectedView,
                position,
                selectedItemId
            )
        }
    }

    override fun setSelection(position: Int) {
        val sameSelected = position == selectedItemPosition
        super.setSelection(position)
        if (sameSelected) {
            //TODO:-> Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
            onItemSelectedListener?.onItemSelected(
                this,
                selectedView,
                position,
                selectedItemId
            )
        }
    }

    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View,
        position: Int,
        id: Long
    ) {
        if (mListener != null) {
            mListener?.onItemSelected(parent, view, position, id, mUserActionOnSpinner)
        }
        // reset variable, so that it will always be true unless tampered with
        mUserActionOnSpinner = true
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        if (mListener != null) mListener?.onNothingSelected(parent)
    }

    fun programmaticallySetPosition(pos: Int, animate: Boolean) {
        mUserActionOnSpinner = false
        setSelection(pos, animate)
    }

    fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        mListener = listener
    }

    interface OnItemSelectedListener {
        /**
         *
         * Callback method to be invoked when an item in this view has been
         * selected. This callback is invoked only when the newly selected
         * position is different from the previously selected position or if
         * there was no selected item.
         *
         *
         * Impelmenters can call getItemAtPosition(position) if they need to access the
         * data associated with the selected item.
         *
         * @param parent   The AdapterView where the selection happened
         * @param view     The view within the AdapterView that was clicked
         * @param position The position of the view in the adapter
         * @param id       The row id of the item that is selected
         */
        fun onItemSelected(
            parent: AdapterView<*>?,
            v: View?,
            position: Int,
            id: Long,
            userSelected: Boolean
        )

        /**
         * Callback method to be invoked when the selection disappears from this
         * view. The selection can disappear for instance when touch is activated
         * or when the adapter becomes empty.
         *
         * @param parent The AdapterView that now contains no selected item.
         */
        fun onNothingSelected(parent: AdapterView<*>?)
    }
}