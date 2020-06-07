package org.xdty.callerinfo.model

class CallRecord {
    private var ring: Long = -1
    var hook: Long = -1
        private set
    private var idle: Long = -1
    private var ringDuration: Long = -1
    private var callDuration: Long = -1
    var logNumber: String? = null
    var logName: String? = null
    var logGeo: String? = null

    fun setLogName(name: String, append: Boolean) {
        if (append) {
            appendName(name)
        } else {
            logName = name
        }
    }

    fun ring() {
        ring = System.currentTimeMillis()
    }

    fun hook() {
        hook = System.currentTimeMillis()
    }

    fun idle() {
        idle = System.currentTimeMillis()
        if (isIncoming) {
            if (hook == -1L) { // missed or hangup incoming call
                ringDuration = idle - ring
                callDuration = 0
            } else { // answered incoming call
                ringDuration = hook - ring
                callDuration = idle - hook
            }
        } else { // outgoing call
            ringDuration = 0
            callDuration = idle - hook
        }
    }

    val isIncoming: Boolean
        get() = ring != -1L

    fun ringDuration(): Long {
        return ringDuration
    }

    fun callDuration(): Long {
        return callDuration
    }

    fun reset() {
        ring = -1
        hook = -1
        idle = -1
        ringDuration = -1
        callDuration = -1
        logNumber = null
        logGeo = null
        logName = null
    }

    fun time(): Long {
        return if (ring != -1L) ring else hook
    }

    fun appendName(s: String) {
        if (logName == null || logName!!.isEmpty()) {
            logName = ""
        }
        logName += " $s"
    }

    val isValid: Boolean
        get() = logNumber != null && !logNumber!!.isEmpty()

    val isNameValid: Boolean
        get() = logName != null && !logName!!.isEmpty()

    val isGeoValid: Boolean
        get() = logGeo != null && !logGeo!!.isEmpty()

    fun matchName(keyword: String?): Boolean {
        return logName != null && logName!!.contains(keyword!!)
    }

    fun matchGeo(keyword: String?): Boolean {
        return logGeo!!.contains(keyword!!)
    }

    fun matchNumber(keyword: String?): Boolean {
        return logNumber != null && logNumber!!.startsWith(keyword!!)
    }

    val isActive: Boolean
        get() = ring != -1L || hook != -1L || idle != -1L

    val isAnswered: Boolean
        get() = isIncoming && callDuration > 0

    fun isEqual(number: String?): Boolean {
        return (logNumber == null && number == null
                || logNumber != null && number != null && logNumber == number)
    }
}