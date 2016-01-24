package org.xdty.callerinfo.plugin;

interface IPluginServiceCallback {
    void onCallPermissionResult(boolean success);
    void onCallLogPermissionResult(boolean success);
}
