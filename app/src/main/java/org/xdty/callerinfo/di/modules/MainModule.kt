package org.xdty.callerinfo.di.modules

import dagger.Module
import dagger.Provides
import org.xdty.callerinfo.contract.MainContract.Presenter
import org.xdty.callerinfo.contract.MainContract.View
import org.xdty.callerinfo.presenter.MainPresenter

@Module
class MainModule(private val mView: View) {
    @Provides
    internal fun provideView(): View {
        return mView
    }

    @Provides
    internal fun providePresenter(): Presenter {
        return MainPresenter(mView)
    }

}