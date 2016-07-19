package org.xdty.callerinfo;

import org.xdty.callerinfo.presenter.MainPresenter;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    void inject(MainPresenter presenter);
}
