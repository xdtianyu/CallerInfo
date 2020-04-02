package org.xdty.callerinfo.utils

import okhttp3.OkHttpClient
import okhttp3.Request

object Networks {

    private const val url_204 = "https://ip.xdty.org/generate_204"

    fun hasNetwork(): Boolean {
        return try {
            val request = Request.Builder()
                    .url(url_204)
                    .build()
            OkHttpClient().newCall(request).execute().code == 204
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}