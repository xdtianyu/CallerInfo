package org.xdty.callerinfo.di.modules;

import org.xdty.callerinfo.contract.MainBottomContact;
import org.xdty.callerinfo.presenter.MainBottomPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public class MainBottomModule {

    private MainBottomContact.View mView;

    public MainBottomModule(MainBottomContact.View view) {
        mView = view;
    }

    @Provides
    MainBottomContact.View provideView() {
        return mView;
    }

    @Provides
    MainBottomContact.Presenter providePresenter() {
        return new MainBottomPresenter(mView);
    }

}
