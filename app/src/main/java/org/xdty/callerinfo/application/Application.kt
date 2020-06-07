package org.xdty.callerinfo.application

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.tencent.bugly.crashreport.CrashReport
import io.reactivex.plugins.RxJavaPlugins
import org.xdty.callerinfo.di.AppComponent
import org.xdty.callerinfo.di.DaggerAppComponent
import org.xdty.callerinfo.di.modules.AppModule
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.utils.Alarm
import org.xdty.callerinfo.utils.Resource
import org.xdty.callerinfo.utils.Utils
import javax.inject.Inject

@SuppressLint("Registered")
open class Application : android.app.Application() {
    lateinit var analytics: FirebaseAnalytics
    @Inject
    internal lateinit var setting: Setting
    @Inject
    internal lateinit var alarm: Alarm

    override fun onCreate() {
        super.onCreate()
        application = this
        RxJavaPlugins.setErrorHandler { throwable -> Log.e(TAG, Log.getStackTraceString(throwable)) }
        init()
    }

    protected fun init() {
        appComponent = DaggerAppComponent.builder().appModule(AppModule(this)).build()
        appComponent.inject(this)
        analytics = FirebaseAnalytics.getInstance(this)
        analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
        Resource.init(Utils.changeLang(this))
        if (!setting.isCatchCrash) {
            CrashReport.initCrashReport(applicationContext, "0eaf845a04", false)
        }
        setting.fix()
        alarm.alarm()
        alarm.enqueueUpgradeWork()
    }

    companion object {
        val TAG = Application::class.java.simpleName
        lateinit var application: Application
        lateinit var appComponent: AppComponent
    }
}