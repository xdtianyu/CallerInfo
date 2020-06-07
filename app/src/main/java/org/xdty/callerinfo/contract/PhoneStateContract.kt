package org.xdty.callerinfo.contract

import android.content.Context
import org.xdty.phone.number.model.INumber

interface PhoneStateContract {
    interface View {
        fun show(number: INumber)
        fun showFailed(isOnline: Boolean)
        fun showSearching()
        fun hide(number: String)
        fun close(number: String)
        val isShowing: Boolean
        val context: Context
        fun showMark(number: String)
    }

    interface Presenter : BasePresenter {
        fun matchIgnore(number: String): Boolean
        fun handleRinging(number: String)
        fun handleOffHook(number: String)
        fun handleIdle(number: String)
        fun resetCallRecord()
        fun checkClose(number: String): Boolean
        fun isIncoming(number: String): Boolean
        fun saveInCall()
        val isRingOnce: Boolean
        fun searchNumber(number: String)
        fun setOutGoingNumber(number: String)
        fun canReadPhoneState(): Boolean
    }
}