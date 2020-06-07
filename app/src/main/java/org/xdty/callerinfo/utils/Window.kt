package org.xdty.callerinfo.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import org.xdty.callerinfo.R
import org.xdty.callerinfo.application.Application
import org.xdty.callerinfo.model.TextColorPair
import org.xdty.callerinfo.service.FloatWindow
import org.xdty.phone.number.model.INumber
import wei.mark.standout.StandOutWindow

class Window {
    private val mContext: Context
    var isShowing = false
        private set

    fun showTextWindow(resId: Int, type: Type) {
        isShowing = true
        val frontType = type.value()
        val bundle = Bundle()
        bundle.putString(FloatWindow.NUMBER_INFO, Resource.resources.getString(resId))
        bundle.putInt(FloatWindow.WINDOW_COLOR, ContextCompat.getColor(mContext,
                R.color.colorPrimary))
        Log.d(TAG, "showTextWindow: " + Utils.bundleToString(bundle))
        StandOutWindow.show(mContext, FloatWindow::class.java, frontType)
        StandOutWindow.sendData(mContext, FloatWindow::class.java,
                frontType, 0, bundle, FloatWindow::class.java, 0)
    }

    fun sendData(key: String?, value: Int, type: Type) {
        Log.d(TAG, "sendData")
        isShowing = true
        val frontType = type.value()
        val bundle = Bundle()
        bundle.putInt(key, value)
        StandOutWindow.show(mContext, FloatWindow::class.java, frontType)
        StandOutWindow.sendData(mContext, FloatWindow::class.java,
                frontType, 0, bundle, FloatWindow::class.java, 0)
    }

    fun closeWindow() {
        Log.d(TAG, "closeWindow")
        if (isShowing) {
            isShowing = false
            StandOutWindow.closeAll(mContext, FloatWindow::class.java)
        }
    }

    fun showWindow(number: INumber?, type: Type) {
        isShowing = true
        val frontType = type.value()
        val textColor = TextColorPair.from(number!!)
        val bundle = Bundle()
        bundle.putString(FloatWindow.NUMBER_INFO, textColor.text)
        bundle.putInt(FloatWindow.WINDOW_COLOR, textColor.color)
        Log.d(TAG, "showWindow: " + Utils.bundleToString(bundle))
        StandOutWindow.show(mContext, FloatWindow::class.java,
                frontType)
        StandOutWindow.sendData(mContext, FloatWindow::class.java,
                frontType, 0, bundle, FloatWindow::class.java, 0)
    }

    fun hideWindow() {
        Log.d(TAG, "hideWindow")
        if (isShowing) {
            StandOutWindow.hide(mContext, FloatWindow::class.java, Type.CALLER.value())
        }
    }

    enum class Type(private val mType: Int) {
        CALLER(FloatWindow.CALLER_FRONT), POSITION(FloatWindow.SET_POSITION_FRONT), SETTING(FloatWindow.SETTING_FRONT), SEARCH(FloatWindow.SEARCH_FRONT);

        fun value(): Int {
            return mType
        }

    }

    companion object {
        private val TAG = Window::class.java.simpleName
    }

    init {
        mContext = Application.application
    }
}