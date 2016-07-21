package org.xdty.callerinfo.di;

import org.xdty.callerinfo.di.modules.PhoneStatusModule;
import org.xdty.callerinfo.receiver.IncomingCall;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = PhoneStatusModule.class)
public interface PhoneStatusComponent {

    void inject(IncomingCall.IncomingCallListener listener);

}
