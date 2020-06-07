package org.xdty.callerinfo.model

import org.xdty.callerinfo.R

enum class MarkType(private val mType: Int) {

    HARASSMENT(0),
    FRAUD(1),
    ADVERTISING(2),
    EXPRESS_DELIVERY(3),
    RESTAURANT_DELIVER(4),
    CUSTOM(5);

    fun toInt(): Int {
        return mType
    }

    companion object {
        fun fromInt(value: Int): MarkType {
            return if (value >= 0 && value < values().size) {
                values()[value]
            } else CUSTOM
        }

        fun fromResourceId(id: Int): MarkType {
            return when (id) {
                R.id.fraud -> FRAUD
                R.id.harassment -> HARASSMENT
                R.id.advertising -> ADVERTISING
                R.id.express -> EXPRESS_DELIVERY
                R.id.restaurant -> RESTAURANT_DELIVER
                else -> CUSTOM
            }
        }
    }

}