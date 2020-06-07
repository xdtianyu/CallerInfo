package org.xdty.callerinfo.service

import android.app.Activity
import android.app.KeyguardManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.TextView
import org.xdty.callerinfo.R
import org.xdty.callerinfo.application.Application.Companion.appComponent
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.utils.Utils
import wei.mark.standout.StandOutWindow
import wei.mark.standout.StandOutWindow.StandOutLayoutParams
import wei.mark.standout.constants.StandOutFlags
import wei.mark.standout.ui.Window
import javax.inject.Inject

class FloatWindow : StandOutWindow() {
    @Inject
    internal lateinit var mSettings: Setting

    private var isFirstShow = false
    private var isFocused = false
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            appComponent.inject(this)
            return super.onStartCommand(intent, flags, startId)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            stopSelf(startId)
        }
        return Service.START_NOT_STICKY
    }

    override fun attachBaseContext(newBase: Context) {
        val context: Context = Utils.changeLang(newBase)
        super.attachBaseContext(context)
    }

    override fun getAppName(): String {
        return resources.getString(R.string.app_name)
    }

    override fun getAppIcon(): Int {
        return R.drawable.status_icon
    }

    override fun createAndAttachView(id: Int, frame: FrameLayout) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.float_window, frame, true)
    }

    // the window will be centered
    override fun getParams(id: Int, window: Window): StandOutLayoutParams {
        val params = StandOutLayoutParams(id, mSettings.screenWidth,
                mSettings.windowHeight, StandOutLayoutParams.CENTER,
                StandOutLayoutParams.CENTER)
        val x = mSettings.windowX
        val y = mSettings.windowY
        if (x != -1 && y != -1) {
            params.x = x
            params.y = y
        }
        if (id == SETTING_FRONT || id == SEARCH_FRONT) {
            params.y = (mSettings.defaultHeight * 1.5).toInt()
        }
        params.minWidth = mSettings.screenWidth
        params.maxWidth = Math.max(mSettings.screenWidth, mSettings.screenHeight)
        params.minHeight = mSettings.defaultHeight / 4
        if (isUnmovable(id)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                params.type = StandOutLayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                params.type = StandOutLayoutParams.TYPE_SYSTEM_OVERLAY
            }
        }
        return params
    }

    // move the window by dragging the view
    override fun getFlags(id: Int): Int {
        return if (isUnmovable(id)) {
            super.getFlags(id) or StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE
        } else {
            (super.getFlags(id) or StandOutFlags.FLAG_BODY_MOVE_ENABLE
                    or StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE
                    or StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE)
        }
    }

    private fun isUnmovable(id: Int): Boolean {
        val km = getSystemService(Activity.KEYGUARD_SERVICE) as KeyguardManager
        return id == SETTING_FRONT || id == SEARCH_FRONT || km.inKeyguardRestrictedInputMode()
    }

    override fun getPersistentNotificationTitle(id: Int): String {
        return appName
    }

    override fun getPersistentNotificationMessage(id: Int): String {
        return getString(R.string.close_float_window)
    }

    override fun getPersistentNotificationIntent(id: Int): Intent {
        return getCloseIntent(this, FloatWindow::class.java, id)
    }

    override fun getCloseAnimation(id: Int): Animation? {
        return if (mSettings.isShowCloseAnim) {
            super.getCloseAnimation(id)
        } else {
            null
        }
    }

    override fun onShow(id: Int, window: Window): Boolean {
        isFirstShow = true
        mShowingStatus = STATUS_SHOWING
        return super.onShow(id, window)
    }

    override fun onMove(id: Int, window: Window, view: View, event: MotionEvent) {
        super.onMove(id, window, view, event)
        mSettings.setWindow(window.layoutParams.x, window.layoutParams.y)
    }

    override fun onClose(id: Int, window: Window): Boolean {
        super.onClose(id, window)
        stopService(getShowIntent(this, javaClass, id))
        mShowingStatus = STATUS_CLOSE
        return false
    }

    override fun onHide(id: Int, window: Window): Boolean {
        mShowingStatus = STATUS_HIDE
        return super.onHide(id, window)
    }

    override fun onTouchBody(id: Int, window: Window, view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_OUTSIDE -> {
                val layout = window.findViewById<View>(R.id.window_layout)
                layout?.setBackgroundResource(0)
                if (!isFocused && mSettings.isHidingWhenTouch && id == CALLER_FRONT && getWindow(id) != null) {
                    hide(id)
                }
                isFocused = false
            }
        }
        return super.onTouchBody(id, window, view, event)
    }

    override fun onReceiveData(id: Int, requestCode: Int, data: Bundle,
                               fromCls: Class<out StandOutWindow>, fromId: Int) {
        val color = data.getInt(WINDOW_COLOR)
        val text = data.getString(NUMBER_INFO)
        var size = data.getInt(TEXT_SIZE)
        val height = data.getInt(WINDOW_HEIGHT)
        var trans = data.getInt(WINDOW_TRANS)
        val error = data.getInt(WINDOW_ERROR)
        var padding = data.getInt(TEXT_PADDING)
        val window = getWindow(id) ?: return
        val layout = window.findViewById<View>(R.id.content)
        val textView = window.findViewById<View>(R.id.number_info) as TextView
        val errorText = window.findViewById<View>(R.id.error) as TextView
        if (padding == 0) {
            padding = mSettings.textPadding
        }
        if (id == CALLER_FRONT || id == SETTING_FRONT) {
            val alignType = mSettings.textAlignment
            val gravity: Int
            when (alignType) {
                TEXT_ALIGN_LEFT -> {
                    gravity = Gravity.START or Gravity.CENTER
                    textView.setPadding(padding, 0, 0, 0)
                }
                TEXT_ALIGN_CENTER -> {
                    gravity = Gravity.CENTER
                    textView.setPadding(0, padding, 0, 0)
                }
                TEXT_ALIGN_RIGHT -> {
                    gravity = Gravity.END or Gravity.CENTER
                    textView.setPadding(0, 0, padding, 0)
                }
                else -> {
                    gravity = Gravity.CENTER
                    textView.setPadding(0, padding, 0, 0)
                }
            }
            errorText.gravity = gravity
            textView.gravity = gravity
        }
        if (size == 0) {
            size = mSettings.textSize
        }
        if (height != 0) {
            val params = window.layoutParams
            window.edit().setSize(params.width, height).commit()
        }
        if (trans == 0) {
            trans = mSettings.windowTransparent
        }
        if (color != 0) {
            layout.setBackgroundColor(color)
            if (mSettings.isEnableTextColor && id == CALLER_FRONT) {
                textView.setTextColor(color)
            }
        }
        if (text != null) {
            textView.text = text
        }
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size.toFloat())
        if (mSettings.isTransBackOnly) {
            if (layout.background != null) {
                layout.background.alpha = (trans / 100.0 * 255).toInt()
            }
        } else {
            layout.alpha = trans / 100f
        }
        if (error != 0) {
            errorText.visibility = View.VISIBLE
            errorText.text = getString(error)
        }
    }

    override fun onFocusChange(id: Int, window: Window, focus: Boolean): Boolean {
        val layout = window.findViewById<View>(R.id.window_layout)
        if (focus && layout != null && !isFirstShow) {
            layout.setBackgroundResource(wei.mark.standout.R.drawable.border_focused)
            isFocused = true
        }
        isFirstShow = false
        return true
    }

    override fun isDisableMove(id: Int): Boolean {
        return id == CALLER_FRONT && mSettings.isDisableMove
    }

    companion object {
        val TAG = FloatWindow::class.java.simpleName
        const val NUMBER_INFO = "number_info"
        const val TEXT_SIZE = "text_size"
        const val TEXT_PADDING = "text_padding"
        const val WINDOW_HEIGHT = "window_height"
        const val WINDOW_TRANS = "window_trans"
        const val WINDOW_COLOR = "window_color"
        const val WINDOW_ERROR = "window_error"
        const val CALLER_FRONT = 1000
        const val SET_POSITION_FRONT = 1001
        const val SETTING_FRONT = 1002
        const val SEARCH_FRONT = 1003
        const val STATUS_CLOSE = 0
        const val TEXT_ALIGN_LEFT = 0
        const val TEXT_ALIGN_CENTER = 1
        const val TEXT_ALIGN_RIGHT = 2
        private const val STATUS_SHOWING = 1
        private const val STATUS_HIDE = 2
        private var mShowingStatus = STATUS_CLOSE
        // TODO: move status at utils
        fun status(): Int {
            return mShowingStatus
        }
    }
}