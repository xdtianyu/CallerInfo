package org.xdty.callerinfo.presenter

import androidx.work.ListenableWorker
import org.xdty.callerinfo.application.Application
import org.xdty.callerinfo.contract.UpgradeContact
import org.xdty.callerinfo.model.Status
import org.xdty.config.Config
import javax.inject.Inject

class UpgradePresenter(view: UpgradeContact.View) : UpgradeContact.Presenter {

    @Inject
    lateinit var mConfig: Config

    private var mView: UpgradeContact.View = view

    init {
        Application.getApplication().appComponent.inject(this)
    }

    override fun start() {

    }

    override fun loadConfig(): ListenableWorker.Result {
        try {
            val status = mConfig.get(Status::class.java, "CallerInfo.json")
            mView.showSucceedNotification(status)
            return ListenableWorker.Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            mView.showFailedNotification(e)
        }
        return ListenableWorker.Result.failure()
    }
}