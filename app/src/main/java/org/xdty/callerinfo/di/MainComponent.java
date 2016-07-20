package org.xdty.callerinfo.di;

import org.xdty.callerinfo.activity.MainActivity;
import org.xdty.callerinfo.di.modules.MainModule;

import dagger.Component;

@Component(modules = MainModule.class)
public interface MainComponent{
    void inject(MainActivity view);
}
