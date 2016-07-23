package org.xdty.callerinfo.utils;

public final class Config {

    public static final long MAX_UPDATE_CIRCLE = 2 * 24 * 3600 * 1000;

    private Config() {
        throw new AssertionError("Config class is not meant to be instantiated or subclassed.");
    }
}
