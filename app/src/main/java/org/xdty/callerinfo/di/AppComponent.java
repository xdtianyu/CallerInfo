package org.xdty.callerinfo.di;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.di.modules.AppModule;
import org.xdty.callerinfo.presenter.MainPresenter;
import org.xdty.callerinfo.presenter.PhoneStatePresenter;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    void inject(MainPresenter presenter);

    void inject(PhoneStatePresenter presenter);

    void inject(Application application);
}
