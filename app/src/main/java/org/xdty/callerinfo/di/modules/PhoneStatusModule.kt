package org.xdty.callerinfo.di.modules

import dagger.Module
import dagger.Provides
import org.xdty.callerinfo.contract.PhoneStateContract.Presenter
import org.xdty.callerinfo.contract.PhoneStateContract.View
import org.xdty.callerinfo.presenter.PhoneStatePresenter
import javax.inject.Singleton

@Module
class PhoneStatusModule(private var mView: View) {
    @Provides
    internal fun provideView(): View {
        return mView
    }

    @Singleton
    @Provides
    internal fun providePresenter(): Presenter {
        return PhoneStatePresenter(mView)
    }

}