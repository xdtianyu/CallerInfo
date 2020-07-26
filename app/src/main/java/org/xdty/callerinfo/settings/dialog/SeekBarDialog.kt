package org.xdty.callerinfo.settings.dialog

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import app.minimize.com.seek_bar_compat.SeekBarCompat
import org.xdty.callerinfo.R

class SeekBarDialog(context: Context, sharedPreferences: SharedPreferences) :
        SettingsDialog(context, sharedPreferences) {

    lateinit var seekBar: SeekBarCompat
    var max: Int = 100
    var defaultValue: Int = 0

    lateinit var listener: SeekListener

    fun max(max: Int): SeekBarDialog {
        this.max = max
        return this
    }

    fun defaultValue(value: Int): SeekBarDialog {
        defaultValue = value
        return this
    }

    fun seek(listener: SeekListener): SeekBarDialog {
        this.listener = listener
        return this
    }

    override fun bindViews() {

        val layout = View.inflate(context, R.layout.dialog_seek, null)
        builder.setView(layout)

        val value = sharedPrefs.getInt(key, defaultValue)
        seekBar = layout.findViewById(R.id.seek_bar)
        seekBar.max = max
        seekBar.progress = value
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                listener.onSeek(progress)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }

    override fun onConfirm() {
        val value = seekBar.progress
        val editor = sharedPrefs.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    abstract class SeekListener {
        abstract fun onSeek(progress: Int)
    }
}