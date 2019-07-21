package org.xdty.callerinfo.model

import androidx.annotation.Keep

@Keep
data class Status(
        var version: Int,
        var count: Int,
        var newCount: Int,
        var timestamp: Long,
        var md5: String,
        var url: String
) {
    override fun toString(): String {
        return "Status(version=$version, count=$count, newCount=$newCount, timestamp=$timestamp, md5='$md5', url='$url')"
    }
}