package org.xdty.callerinfo.di.modules

import dagger.Module
import dagger.Provides
import io.requery.Persistable
import io.requery.android.sqlite.DatabaseSource
import io.requery.sql.ConfigurationBuilder
import io.requery.sql.EntityDataStore
import okhttp3.OkHttpClient
import org.xdty.callerinfo.BuildConfig
import org.xdty.callerinfo.application.Application
import org.xdty.callerinfo.data.CallerDataSource
import org.xdty.callerinfo.data.CallerRepository
import org.xdty.callerinfo.model.database.Database
import org.xdty.callerinfo.model.database.DatabaseImpl
import org.xdty.callerinfo.model.db.Models
import org.xdty.callerinfo.model.permission.Permission
import org.xdty.callerinfo.model.permission.PermissionImpl
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.model.setting.SettingImpl
import org.xdty.callerinfo.utils.Alarm
import org.xdty.callerinfo.utils.Constants
import org.xdty.callerinfo.utils.Contact
import org.xdty.callerinfo.utils.Window
import org.xdty.config.Config
import org.xdty.config.Config.Builder
import org.xdty.phone.number.RxPhoneNumber
import org.xdty.phone.number.util.OkHttp
import javax.inject.Singleton

@Module
class AppModule(var app: Application) {
    @Singleton
    @Provides
    fun provideApplication(): Application {
        return app
    }

    @Singleton
    @Provides
    fun providePhoneNumber(): RxPhoneNumber {
        return RxPhoneNumber(app)
    }

    @Singleton
    @Provides
    fun provideSetting(): Setting {
        SettingImpl.init(app)
        return SettingImpl.instance
    }

    @Singleton
    @Provides
    fun provideDatabase(): Database {
        return DatabaseImpl.instance
    }

    @Singleton
    @Provides
    fun provideDatabaseSource(): EntityDataStore<Persistable> {
        val source: DatabaseSource = object : DatabaseSource(app, Models.DEFAULT, Constants.DB_NAME, Constants.DB_VERSION) {
            override fun onConfigure(builder: ConfigurationBuilder) {
                super.onConfigure(builder)
                builder.setQuoteColumnNames(true)
            }
        }
        source.setLoggingEnabled(BuildConfig.DEBUG)
        val configuration = source.configuration
        return EntityDataStore(configuration)
    }

    @Singleton
    @Provides
    fun providePermission(): Permission {
        return PermissionImpl(app)
    }

    @Singleton
    @Provides
    fun provideAlarm(): Alarm {
        return Alarm()
    }

    @Singleton
    @Provides
    fun provideWindow(): Window {
        return Window()
    }

    @Singleton
    @Provides
    fun provideContact(): Contact {
        return Contact.instance
    }

    @Singleton
    @Provides
    fun provideCallerDataSource(): CallerDataSource {
        return CallerRepository()
    }

    @Singleton
    @Provides
    fun provideConfig(): Config {
        return Builder()
                .endpoint("https://s3-hk.xdty.org")
                .accessKey("vAuKLADukB690VXlOr")
                .secretKey("bSi9tMs8pdWNgGpgYht5lxDWf76SAg5sdR5U")
                .build()
    }

    @Singleton
    @Provides
    fun provideOkHttp(): OkHttpClient {
        return OkHttp.get().client()
    }

}