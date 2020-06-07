package org.xdty.callerinfo.utils

class Config private constructor() {
    companion object {
        const val MAX_UPDATE_CIRCLE = 2 * 24 * 3600 * 1000.toLong()
    }

    init {
        throw AssertionError("Config class is not meant to be instantiated or subclassed.")
    }
}