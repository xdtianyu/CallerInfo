package org.xdty.callerinfo.model

class CallRecord {
    private var ring: Long = -1
    var hook: Long = -1
        private set
    private var idle: Long = -1
    private var ringDuration: Long = -1
    private var callDuration: Long = -1
    var logNumber: String = ""
    var logName: String = ""
    var logGeo: String = ""

    val isIncoming: Boolean
        get() = ring != -1L

    val isValid: Boolean
        get() = logNumber.isNotEmpty()

    val isNameValid: Boolean
        get() = logName.isNotEmpty()

    val isGeoValid: Boolean
        get() = logGeo.isNotEmpty()

    val isActive: Boolean
        get() = ring != -1L || hook != -1L || idle != -1L

    val isAnswered: Boolean
        get() = isIncoming && callDuration > 0

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
        logNumber = ""
        logGeo = ""
        logName = ""
    }

    fun time(): Long {
        return if (ring != -1L) ring else hook
    }

    private fun appendName(s: String) {
        logName += " $s"
    }

    fun matchName(keyword: String?): Boolean {
        return logName.contains(keyword!!)
    }

    fun matchGeo(keyword: String?): Boolean {
        return logGeo.contains(keyword!!)
    }

    fun matchNumber(keyword: String?): Boolean {
        return logNumber.startsWith(keyword!!)
    }

    fun isEqual(number: String?): Boolean {
        return (number == null || logNumber == number)
    }
}