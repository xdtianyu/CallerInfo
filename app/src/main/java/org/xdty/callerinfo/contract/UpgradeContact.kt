package org.xdty.callerinfo.contract

import android.content.Context
import androidx.work.ListenableWorker
import org.xdty.callerinfo.model.Status

interface UpgradeContact {

    interface View : BaseView<Presenter> {
        fun showSucceedNotification(status: Status)

        fun showFailedNotification(error: Exception)
    }

    interface Presenter : BasePresenter {
        fun upgradeOfflineData(context: Context): ListenableWorker.Result
    }
}