package org.xdty.callerinfo.plugin;

import org.xdty.callerinfo.plugin.IPluginServiceCallback;

interface IPluginService {
    void checkCallPermission();
    void checkCallLogPermission();
    void hangUpPhoneCall();
    void registerCallback(IPluginServiceCallback callback);
}
