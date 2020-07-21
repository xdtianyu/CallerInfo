package org.xdty.callerinfo.settings.dialog

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.TextView
import org.xdty.callerinfo.R

class TextDialog(context: Context, sharedPrefs: SharedPreferences) : SettingsDialog(context, sharedPrefs) {

    override fun bindViews() {
        val layout = View.inflate(context, R.layout.dialog_text, null)
        builder.setView(layout)

        val textView: TextView = layout.findViewById(R.id.text)
        textView.text = text
    }

    override fun onConfirm() {

    }

    override fun positive(): Boolean {
        return false
    }

    override fun negative(): Boolean {
        return false
    }
}