package org.xdty.callerinfo.receiver

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import org.xdty.callerinfo.BuildConfig
import org.xdty.callerinfo.R
import org.xdty.callerinfo.application.Application.Companion.application
import org.xdty.callerinfo.contract.PhoneStateContract.Presenter
import org.xdty.callerinfo.contract.PhoneStateContract.View
import org.xdty.callerinfo.di.DaggerPhoneStatusComponent
import org.xdty.callerinfo.di.modules.AppModule
import org.xdty.callerinfo.di.modules.PhoneStatusModule
import org.xdty.callerinfo.service.FloatWindow
import org.xdty.callerinfo.utils.Utils
import org.xdty.callerinfo.utils.Window
import org.xdty.phone.number.model.INumber
import javax.inject.Inject

class IncomingCall : BroadcastReceiver() {
    private val mPhoneStateListener: PhoneStateListener
    override fun onReceive(context: Context, intent: Intent) {
        mPhoneStateListener.setContext(context)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onReceive: " + intent.toString() + " " +
                    Utils.bundleToString(intent.extras))
        }
        val action = intent.action
        if (action != null) {
            when (action) {
                Intent.ACTION_NEW_OUTGOING_CALL -> if (intent.extras != null) {
                    mPhoneStateListener.setOutGoingNumber(
                            intent.extras!!.getString(Intent.EXTRA_PHONE_NUMBER))
                }
                TelephonyManager.ACTION_PHONE_STATE_CHANGED -> if (intent.extras != null) {
                    val state = intent.extras!!.getString(TelephonyManager.EXTRA_STATE)
                    val number = intent.extras!!
                            .getString(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    mPhoneStateListener.onCallStateChanged(state, number)
                }
            }
        }
    }

    class PhoneStateListener private constructor() : View {
        @Inject
        lateinit var mPresenter: Presenter
        @Inject
        lateinit var mWindow: Window
        
        override lateinit var context: Context
            private set

        fun setContext(context: Context) {
            this.context = context.applicationContext
        }

        fun setOutGoingNumber(number: String?) {
            mPresenter.setOutGoingNumber(number!!)
            onCallStateChanged(TelephonyManager.EXTRA_STATE_OFFHOOK, number)
        }

        fun onCallStateChanged(state: Int, number: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> onCallStateChanged(TelephonyManager.EXTRA_STATE_RINGING, number)
                TelephonyManager.CALL_STATE_OFFHOOK -> onCallStateChanged(TelephonyManager.EXTRA_STATE_OFFHOOK, number)
                TelephonyManager.CALL_STATE_IDLE -> onCallStateChanged(TelephonyManager.EXTRA_STATE_IDLE, number)
            }
        }

        fun onCallStateChanged(state: String?, number: String?) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onCallStateChanged: $state : $number")
                Log.d(TAG, "onCallStateChanged: permission -> " + mPresenter.canReadPhoneState())
            }
            if (mPresenter.matchIgnore(number!!)) {
                return
            }
            when (state) {
                "RINGING" -> mPresenter.handleRinging(number)
                "OFFHOOK" -> mPresenter.handleOffHook(number)
                "IDLE" -> mPresenter.handleIdle(number)
            }
        }

        override fun show(number: INumber) {
            mWindow.showWindow(number, Window.Type.CALLER)
        }

        override fun showFailed(isOnline: Boolean) {
            if (isOnline) {
                mWindow.sendData(FloatWindow.WINDOW_ERROR,
                        R.string.online_failed, Window.Type.CALLER)
            } else {
                mWindow.showTextWindow(R.string.offline_failed, Window.Type.CALLER)
            }
        }

        override fun showSearching() {
            mWindow.showTextWindow(R.string.searching, Window.Type.CALLER)
        }

        override fun hide(number: String) {
            mWindow.hideWindow()
        }

        override fun close(number: String) {
            mWindow.closeWindow()
        }

        override val isShowing: Boolean
            get() = mWindow.isShowing

        override fun showMark(number: String) {
            val keyguardManager = context.getSystemService(
                    Context.KEYGUARD_SERVICE) as KeyguardManager
            val isKeyguardLocked: Boolean
            isKeyguardLocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                keyguardManager.isKeyguardLocked
            } else {
                keyguardManager.inKeyguardRestrictedInputMode()
            }
            if (isKeyguardLocked) {
                Utils.showMarkNotification(context, number)
            } else {
                Utils.startMarkActivity(context, number)
            }
        }

        private object SingletonHelper {
            @SuppressLint("StaticFieldLeak")
            val sINSTANCE = PhoneStateListener()
        }

        companion object {
            val instance: PhoneStateListener
                get() = SingletonHelper.sINSTANCE
        }

        init {
            try {
                DaggerPhoneStatusComponent.builder()
                        .appModule(AppModule(application))
                        .phoneStatusModule(PhoneStatusModule(this))
                        .build()
                        .inject(this)
                mPresenter.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private val TAG = IncomingCall::class.java.simpleName
    }

    init {
        mPhoneStateListener = PhoneStateListener.instance
    }
}