package org.xdty.callerinfo.di

import dagger.Component
import org.xdty.callerinfo.di.modules.AppModule
import org.xdty.callerinfo.di.modules.UpgradeModule
import org.xdty.callerinfo.worker.UpgradeWorker
import javax.inject.Singleton


@Singleton
@Component(modules = [AppModule::class, UpgradeModule::class])
interface UpgradeComponent {
    fun inject(upgradeWorker: UpgradeWorker)
}
