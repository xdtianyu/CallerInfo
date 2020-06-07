package org.xdty.callerinfo.di

import dagger.Component
import org.xdty.callerinfo.activity.MainActivity
import org.xdty.callerinfo.di.modules.AppModule
import org.xdty.callerinfo.di.modules.MainModule
import javax.inject.Singleton

@Singleton
@Component(modules = [MainModule::class, AppModule::class])
interface MainComponent {
    fun inject(view: MainActivity)
}