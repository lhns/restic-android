package de.lolhens.resticui.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import de.lolhens.resticui.R

object InputDialogUtil {
    fun showInputTextDialog(
        context: Context,
        view: View,
        title: String,
        value: String,
        onConfirm: (String) -> Unit
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle(title)

        val viewInflated: View = LayoutInflater.from(context)
            .inflate(R.layout.dialog_input_text, view as ViewGroup?, false)

        val input = viewInflated.findViewById<View>(R.id.input) as EditText

        input.setText(value)
        input.requestFocus()

        builder.setPositiveButton(context.resources.getString(R.string.button_ok)) { dialog, which ->
            onConfirm(
                input.text.toString()
            )
        }

        builder.setNegativeButton(context.resources.getString(R.string.button_cancel)) { dialog, which ->
            dialog.cancel()
        }

        builder.setView(viewInflated)

        builder.show()
    }
}