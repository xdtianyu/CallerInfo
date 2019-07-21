package org.xdty.callerinfo.di.modules

import dagger.Module
import dagger.Provides
import org.xdty.callerinfo.contract.UpgradeContact
import org.xdty.callerinfo.presenter.UpgradePresenter

@Module
class UpgradeModule(view: UpgradeContact.View) {
    private var mView: UpgradeContact.View = view

    @Provides
    fun provideView(): UpgradeContact.View {
        return mView
    }

    @Provides
    fun providePresenter(): UpgradeContact.Presenter {
        return UpgradePresenter(mView)
    }
}