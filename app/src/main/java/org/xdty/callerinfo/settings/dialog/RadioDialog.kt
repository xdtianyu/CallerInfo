package org.xdty.callerinfo.settings.dialog

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import org.xdty.callerinfo.R
import java.util.*

class RadioDialog(context: Context, sharedPrefs: SharedPreferences) : SettingsDialog(context, sharedPrefs) {

    private var defaultValue: Int = 0
    private var listId: Int = 0
    private var offset: Int = 0
    lateinit var listener: CheckedListener

    override fun bindViews() {
        val layout = View.inflate(context, R.layout.dialog_radio, null)
        builder.setView(layout)

        val radioGroup: RadioGroup = layout.findViewById(R.id.radio)
        val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)


        val list = Arrays.asList(*context.resources.getStringArray(listId))

        for (s in list) {
            val radioButton = RadioButton(context)
            radioButton.text = s
            radioGroup.addView(radioButton, layoutParams)
        }

        val button = radioGroup.getChildAt(
                sharedPrefs.getInt(key, defaultValue) - offset) as RadioButton

        button.isChecked = true
        button.setOnClickListener { dialog.dismiss() }


        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val index = group.indexOfChild(group.findViewById(checkedId))
            val editor = sharedPrefs.edit()
            editor.putInt(key, index + offset)
            editor.apply()
            dialog.dismiss()

            listener.onChecked(list[index])
        }
    }

    override fun onConfirm() {

    }

    fun defaultValue(defaultValue: Int): RadioDialog {
        this.defaultValue = defaultValue
        return this
    }

    fun offset(offset: Int): RadioDialog {
        this.offset = offset
        return this
    }

    fun listId(listId: Int): RadioDialog {
        this.listId = listId
        return this
    }

    fun check(listener: CheckedListener): RadioDialog {
        this.listener = listener
        return this
    }

    fun interface CheckedListener {
        fun onChecked(value: String)
    }
}