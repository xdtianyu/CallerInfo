package org.xdty.callerinfo.settings.dialog

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.view.View
import androidx.appcompat.app.AlertDialog
import org.xdty.callerinfo.R

abstract class SettingsDialog(protected var context: Context, protected var sharedPrefs: SharedPreferences) {

    protected var builder: AlertDialog.Builder = AlertDialog.Builder(context)

    protected lateinit var key: String
    protected lateinit var layout: View

    protected var hint = 0
    protected var defaultText = 0
    protected var help = 0
    protected var text = ""

    private var confirmListener: ConfirmListener? = null
    private var cancelListener: CancelListener? = null
    private var helpListener: HelpListener? = null

    fun key(keyId: Int): SettingsDialog {
        key = context.getString(keyId)
        return this
    }

    fun key(key: String): SettingsDialog {
        this.key = key
        return this
    }

    fun title(titleId: Int): SettingsDialog {
        builder.setTitle(context.getString(titleId))
        return this
    }

    fun hint(hint: Int): SettingsDialog {
        this.hint = hint
        return this
    }

    fun defaultText(defaultText: Int): SettingsDialog {
        this.defaultText = defaultText
        return this
    }

    fun text(text: String): SettingsDialog {
        this.text = text
        return this
    }

    fun help(help: Int): SettingsDialog {
        this.help = help
        return this
    }

    fun confirm(listener: ConfirmListener?): SettingsDialog {
        this.confirmListener = listener
        return this
    }

    fun cancel(listener: CancelListener?): SettingsDialog {
        this.cancelListener = listener
        return this
    }

    fun help(listener: HelpListener?): SettingsDialog {
        this.helpListener = listener
        return this
    }

    fun show() {
        bindViews()

        builder.setPositiveButton(R.string.ok, confirmListener)

        if (cancelListener != null) {
            builder.setNegativeButton(R.string.cancel, cancelListener)
        }

        if (help != 0) {
            builder.setNeutralButton(help) { _: DialogInterface?, _: Int -> onHelp() }
        }
        builder.setCancelable(true)
        builder.show()
    }

    protected abstract fun bindViews()
    protected abstract fun onConfirm()

    protected fun onConfirm(value: String?) {
        confirmListener?.onConfirm(value)
    }

    protected fun onCancel() {
        cancelListener?.onCancel()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun onHelp() {
        helpListener?.onHelp()
    }

    open class Listener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface?, id: Int) {

        }
    }

    open class ConfirmListener : Listener() {
        open fun onConfirm(value: String?) {}
    }

    open class CancelListener : Listener() {
        open fun onCancel() {}
    }

    open class HelpListener : Listener() {
        open fun onHelp() {}
    }
}