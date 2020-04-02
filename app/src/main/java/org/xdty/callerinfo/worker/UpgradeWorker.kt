package org.xdty.callerinfo.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.xdty.callerinfo.R
import org.xdty.callerinfo.activity.MainActivity
import org.xdty.callerinfo.application.Application
import org.xdty.callerinfo.contract.UpgradeContact
import org.xdty.callerinfo.di.DaggerUpgradeComponent
import org.xdty.callerinfo.di.modules.AppModule
import org.xdty.callerinfo.di.modules.UpgradeModule
import org.xdty.callerinfo.model.Status
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.utils.Constants
import org.xdty.callerinfo.utils.Networks
import org.xdty.callerinfo.utils.Toasts
import org.xdty.callerinfo.utils.Utils
import javax.inject.Inject

class UpgradeWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams), UpgradeContact.View {

    private val tag = UpgradeWorker::class.java.simpleName

    @Inject
    lateinit var mPresenter: UpgradeContact.Presenter

    @Inject
    lateinit var mSetting: Setting

    init {
        DaggerUpgradeComponent.builder()
                .appModule(AppModule(Application.getApplication()))
                .upgradeModule(UpgradeModule(this))
                .build()
                .inject(this)
    }

    override fun doWork(): Result {
        return if (Networks.hasNetwork()) {
            mPresenter.upgradeOfflineData(applicationContext)
        } else {
            Toasts.show(applicationContext, R.string.check_offline_no_network)
            Result.failure()
        }
    }

    override fun setPresenter(presenter: UpgradeContact.Presenter) {
        mPresenter = presenter
    }

    override fun showSucceedNotification(status: Status) {
        val info = applicationContext.getString(R.string.offline_data_version_summary, status.version,
                status.count, Utils.getDate(status.timestamp * 1000))
        showNotification(applicationContext, applicationContext.getString(R.string.offline_data_upgrade_success), info)
    }

    override fun showFailedNotification(error: Exception) {
        showNotification(applicationContext, applicationContext.getString(R.string.offline_data_upgrade_failed), error.message)
    }

    private fun showNotification(context: Context, title: String, message: String?) {
        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val name = Constants.VERBOSE_NOTIFICATION_CHANNEL_NAME
            val description = Constants.VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(Constants.CHANNEL_ID, name, importance)
            channel.description = description

            // Add the channel
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }

        val intent = PendingIntent.getActivity(applicationContext, 0,
                Intent(applicationContext, MainActivity::class.java), 0)

        // Create the notification
        val builder = NotificationCompat.Builder(context, Constants.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call_disconnected_24dp)
                .setContentIntent(intent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle(title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setVibrate(LongArray(0))

        // Show the notification
        NotificationManagerCompat.from(context).notify(Constants.NOTIFICATION_ID, builder.build())
    }
}