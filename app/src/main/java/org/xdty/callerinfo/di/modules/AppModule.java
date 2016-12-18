package org.xdty.callerinfo.di.modules;

import org.xdty.callerinfo.BuildConfig;
import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.data.CallerDataSource;
import org.xdty.callerinfo.data.CallerRepository;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.database.DatabaseImpl;
import org.xdty.callerinfo.model.db.Models;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.permission.PermissionImpl;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.callerinfo.utils.Alarm;
import org.xdty.callerinfo.utils.Contact;
import org.xdty.callerinfo.utils.Window;
import org.xdty.phone.number.PhoneNumber;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.requery.Persistable;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.EntityDataStore;

import static org.xdty.callerinfo.utils.Constants.DB_NAME;
import static org.xdty.callerinfo.utils.Constants.DB_VERSION;

@Module
public class AppModule {

    private Application app;

    public AppModule(Application application) {
        app = application;
    }

    @Singleton
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

    @Singleton
    @Provides
    public Setting provideSetting() {
        SettingImpl.init(app);
        return SettingImpl.getInstance();
    }

    @Singleton
    @Provides
    public Database provideDatabase() {
        return DatabaseImpl.getInstance();
    }

    @Singleton
    @Provides
    public EntityDataStore<Persistable> provideDatabaseSource() {

        DatabaseSource source = new DatabaseSource(app, Models.DEFAULT, DB_NAME, DB_VERSION) {
            @Override
            protected void onConfigure(ConfigurationBuilder builder) {
                super.onConfigure(builder);
                builder.setQuoteColumnNames(true);
            }
        };
        source.setLoggingEnabled(BuildConfig.DEBUG);
        Configuration configuration = source.getConfiguration();

        return new EntityDataStore<>(configuration);
    }

    @Singleton
    @Provides
    public Permission providePermission() {
        return new PermissionImpl(app);
    }

    @Singleton
    @Provides
    public Alarm provideAlarm() {
        return new Alarm();
    }

    @Singleton
    @Provides
    public Window provideWindow() {
        return new Window();
    }

    @Singleton
    @Provides
    public Contact provideContact() {
        return new Contact();
    }

    @Singleton
    @Provides
    public CallerDataSource provideCallerDataSource() {
        return new CallerRepository();
    }

}
