package org.xdty.callerinfo.di.modules;

import org.xdty.callerinfo.contract.PhoneStateContract;
import org.xdty.callerinfo.presenter.PhoneStatePresenter;

import dagger.Module;
import dagger.Provides;

@Module
public class PhoneStatusModule {

    PhoneStateContract.View mView;

    public PhoneStatusModule(PhoneStateContract.View view) {
        mView = view;
    }

    @Provides
    PhoneStateContract.View provideView() {
        return mView;
    }

    @Provides
    PhoneStateContract.Presenter providePresenter() {
        return new PhoneStatePresenter(mView);
    }
}
