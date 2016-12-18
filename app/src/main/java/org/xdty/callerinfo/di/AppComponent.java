package org.xdty.callerinfo.di;

import org.xdty.callerinfo.activity.MarkActivity;
import org.xdty.callerinfo.activity.SettingsActivity;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.data.CallerRepository;
import org.xdty.callerinfo.di.modules.AppModule;
import org.xdty.callerinfo.model.database.DatabaseImpl;
import org.xdty.callerinfo.presenter.MainBottomPresenter;
import org.xdty.callerinfo.presenter.MainPresenter;
import org.xdty.callerinfo.presenter.PhoneStatePresenter;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.callerinfo.service.ScheduleService;
import org.xdty.callerinfo.utils.Alarm;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    void inject(MainPresenter presenter);

    void inject(PhoneStatePresenter presenter);

    void inject(Application application);

    void inject(ScheduleService service);

    void inject(FloatWindow service);

    void inject(Alarm alarm);

    void inject(MarkActivity markActivity);

    void inject(SettingsActivity.SettingsFragment settingsFragment);

    void inject(CallerRepository callerRepository);

    void inject(MainBottomPresenter mainBottomPresenter);

    void inject(DatabaseImpl database);
}
