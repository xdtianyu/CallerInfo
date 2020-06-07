package org.xdty.callerinfo.di

import dagger.Component
import org.xdty.callerinfo.activity.MarkActivity
import org.xdty.callerinfo.application.Application
import org.xdty.callerinfo.data.CallerRepository
import org.xdty.callerinfo.di.modules.AppModule
import org.xdty.callerinfo.fragment.SettingsFragment
import org.xdty.callerinfo.model.database.DatabaseImpl
import org.xdty.callerinfo.presenter.MainBottomPresenter
import org.xdty.callerinfo.presenter.MainPresenter
import org.xdty.callerinfo.presenter.PhoneStatePresenter
import org.xdty.callerinfo.presenter.UpgradePresenter
import org.xdty.callerinfo.service.FloatWindow
import org.xdty.callerinfo.service.ScheduleService
import org.xdty.callerinfo.utils.Alarm
import org.xdty.callerinfo.utils.Contact
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(presenter: MainPresenter)
    fun inject(presenter: PhoneStatePresenter)
    fun inject(application: Application)
    fun inject(service: ScheduleService)
    fun inject(service: FloatWindow)
    fun inject(alarm: Alarm)
    fun inject(markActivity: MarkActivity)
    fun inject(settingsFragment: SettingsFragment)
    fun inject(callerRepository: CallerRepository)
    fun inject(mainBottomPresenter: MainBottomPresenter)
    fun inject(database: DatabaseImpl)
    fun inject(contact: Contact)
    fun inject(upgradePresenter: UpgradePresenter)
}