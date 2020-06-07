package org.xdty.callerinfo.di.modules

import dagger.Module
import dagger.Provides
import org.xdty.callerinfo.contract.MainBottomContact.Presenter
import org.xdty.callerinfo.contract.MainBottomContact.View
import org.xdty.callerinfo.presenter.MainBottomPresenter

@Module
class MainBottomModule(private val mView: View) {
    @Provides
    internal fun provideView(): View {
        return mView
    }

    @Provides
    internal fun providePresenter(): Presenter {
        return MainBottomPresenter(mView)
    }

}