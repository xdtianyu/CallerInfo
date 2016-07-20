package org.xdty.callerinfo.di;

import org.xdty.callerinfo.activity.MainActivity;
import org.xdty.callerinfo.di.modules.AppModule;
import org.xdty.callerinfo.di.modules.MainModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = { MainModule.class, AppModule.class })
public interface MainComponent {
    void inject(MainActivity view);
}
