package org.xdty.callerinfo.contract

import androidx.work.ListenableWorker
import org.xdty.callerinfo.model.Status

interface UpgradeContact {

    interface View : BaseView<Presenter> {
        fun showSucceedNotification(status: Status)

        fun showFailedNotification(error: Exception)
    }

    interface Presenter : BasePresenter {
        fun loadConfig(): ListenableWorker.Result
    }
}