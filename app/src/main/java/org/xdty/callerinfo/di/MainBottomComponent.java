package org.xdty.callerinfo.di;

import org.xdty.callerinfo.di.modules.AppModule;
import org.xdty.callerinfo.di.modules.MainBottomModule;
import org.xdty.callerinfo.fragment.MainBottomSheetFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = { MainBottomModule.class, AppModule.class })
public interface MainBottomComponent {

    void inject(MainBottomSheetFragment mainBottomSheetFragment);
}
