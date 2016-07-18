package org.xdty.callerinfo;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.phone.number.PhoneNumber;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    private Application app;

    public AppModule(Application application) {
        app = application;
    }

    @Provides
    public Application provideApplication() {
        return app;
    }

    @Singleton
    @Provides
    public PhoneNumber providePhoneNumber() {
        PhoneNumber.init(app);
        return PhoneNumber.getInstance();
    }

    @Provides
    public Setting provideSetting() {
        SettingImpl.init(app);
        return SettingImpl.getInstance();
    }

}
