package org.xdty.callerinfo.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.WindowManager
import android.widget.FrameLayout
import com.pkmmte.view.CircularImageView
import org.xdty.callerinfo.R
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.model.setting.SettingImpl
import org.xdty.callerinfo.utils.Utils
import wei.mark.standout.StandOutWindow
import wei.mark.standout.StandOutWindow.StandOutLayoutParams
import wei.mark.standout.constants.StandOutFlags
import wei.mark.standout.ui.Window

// MarkWindow is not used
class MarkWindow : StandOutWindow() {
    private var mWindowManager: WindowManager? = null
    private var mSettings: Setting? = null
    override fun onCreate() {
        super.onCreate()
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            mSettings = SettingImpl.instance
            return super.onStartCommand(intent, flags, startId)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            stopSelf(startId)
        }
        return Service.START_NOT_STICKY
    }

    override fun getAppName(): String {
        return resources.getString(R.string.mark_window)
    }

    override fun getAppIcon(): Int {
        return R.drawable.status_icon
    }

    override fun createAndAttachView(id: Int, frame: FrameLayout) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.mark_window, frame, true)
        bindCircleImage(view, R.id.express)
        bindCircleImage(view, R.id.takeout)
        bindCircleImage(view, R.id.selling)
        bindCircleImage(view, R.id.harass)
        bindCircleImage(view, R.id.bilk)
    }

    override fun attachBaseContext(newBase: Context) {
        val context: Context = Utils.changeLang(newBase)
        super.attachBaseContext(context)
    }

    private fun bindCircleImage(view: View, id: Int) {
        val circularImageView = view.findViewById<View>(id) as CircularImageView
        circularImageView.onFocusChangeListener = OnFocusChangeListener { v, hasFocus -> Log.e(TAG, "$v: $hasFocus") }
        circularImageView.setOnClickListener { v -> Log.e(TAG, v.toString() + "xxx") }
    }

    override fun onMove(id: Int, window: Window, view: View, event: MotionEvent) {
        super.onMove(id, window, view, event)
        val x = window.layoutParams.x
        val width = mSettings!!.screenWidth
        val layout = window.findViewById<View>(R.id.content)
        val alpha = ((width - Math.abs(x) * 1.2) / width).toFloat()
        layout.alpha = alpha
        when (event.action) {
            MotionEvent.ACTION_UP -> if (alpha < 0.6) {
                hide(id)
            } else {
                reset(id)
                layout.alpha = 1.0f
            }
        }
    }

    fun reset(id: Int) {
        val window = getWindow(id)
        mWindowManager!!.updateViewLayout(window, getParams(id, window))
    }

    override fun getParams(id: Int, window: Window): StandOutLayoutParams {
        val params = StandOutLayoutParams(id, mSettings!!.screenWidth,
                mSettings!!.windowHeight, StandOutLayoutParams.CENTER,
                StandOutLayoutParams.CENTER)
        val x = mSettings!!.windowX
        val y = mSettings!!.windowY
        if (x != -1 && y != -1) {
            params.x = x
            params.y = y
        }
        params.y = (mSettings!!.defaultHeight * 1.5).toInt()
        params.minWidth = mSettings!!.screenWidth
        params.maxWidth = Math.max(mSettings!!.screenWidth, mSettings!!.screenHeight)
        params.minHeight = mSettings!!.defaultHeight * 2
        params.height = mSettings!!.defaultHeight * 5
        return params
    }

    override fun getFlags(id: Int): Int {
        return (StandOutFlags.FLAG_BODY_MOVE_ENABLE
                or StandOutFlags.FLAG_WINDOW_FOCUS_INDICATOR_DISABLE)
    }

    override fun getThemeStyle(): Int {
        return R.style.AppTheme
    }

    override fun isDisableMove(id: Int): Boolean {
        return false
    }

    companion object {
        private val TAG = MarkWindow::class.java.simpleName
    }
}