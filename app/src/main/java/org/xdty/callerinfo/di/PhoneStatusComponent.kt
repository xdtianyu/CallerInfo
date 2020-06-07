package org.xdty.callerinfo.di

import dagger.Component
import org.xdty.callerinfo.di.modules.AppModule
import org.xdty.callerinfo.di.modules.PhoneStatusModule
import org.xdty.callerinfo.receiver.IncomingCall.PhoneStateListener
import javax.inject.Singleton

@Singleton
@Component(modules = [PhoneStatusModule::class, AppModule::class])
interface PhoneStatusComponent {
    fun inject(phoneStateListener: PhoneStateListener)
}