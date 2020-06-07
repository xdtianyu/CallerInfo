package org.xdty.callerinfo.utils

import android.content.Context
import android.content.res.Resources

object Resource {
    lateinit var resources: Resources

    fun init(context: Context) {
        resources = context.resources
    }
}