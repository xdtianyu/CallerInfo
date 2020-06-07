package org.xdty.callerinfo.model

import android.content.Context
import android.preference.PreferenceManager
import android.text.TextUtils
import androidx.core.content.ContextCompat
import org.xdty.callerinfo.R
import org.xdty.callerinfo.application.Application.Companion.application
import org.xdty.callerinfo.utils.Resource
import org.xdty.callerinfo.utils.Utils
import org.xdty.phone.number.model.INumber
import org.xdty.phone.number.model.Type

class TextColorPair private constructor() {
    var text = ""
    var color = R.color.blue_light

    companion object {
        // generate color from name, has no geo info.
        fun from(name: String): TextColorPair {
            val type = Utils.markTypeFromName(name).text
            return from(type, "", "", "", name, 0)
        }

        fun from(number: INumber): TextColorPair {
            val province = number.province
            val city = number.city
            val operators = number.provider
            return from(number.type.text, province, city, operators, number.name,
                    number.count)
        }

        private fun from(type: String, province: String,
                         city: String, operators: String, name: String?, count: Int): TextColorPair {
            var province: String? = province
            var city: String? = city
            var operators: String? = operators
            val context: Context = application
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (province == null) {
                province = ""
            }
            if (city == null) {
                city = ""
            }
            if (operators == null) {
                operators = ""
            }
            if (!TextUtils.isEmpty(province) && !TextUtils.isEmpty(city) && province == city) {
                city = ""
            }
            val t = TextColorPair()
            var numberType = Type.fromString(type)
            if (name != null && !name.isEmpty()) {
                numberType = Utils.markTypeFromName(name)
            }
            when (numberType) {
                Type.NORMAL -> {
                    t.text = context.resources.getString(
                            R.string.text_normal, province, city, operators)
                    t.color = preferences.getInt("color_normal",
                            ContextCompat.getColor(context, R.color.blue_light))
                }
                Type.POI -> {
                    t.color = preferences.getInt("color_poi",
                            ContextCompat.getColor(context, R.color.orange_dark))
                    t.text = Resource.resources.getString(
                            R.string.text_poi, province, city, operators, name)
                }
                Type.REPORT -> {
                    t.color = preferences.getInt("color_report",
                            ContextCompat.getColor(context, R.color.red_light))
                    if (count == 0) {
                        t.text = context.resources.getString(
                                R.string.text_poi, province, city, operators, name)
                    } else {
                        t.text = Resource.resources.getString(
                                R.string.text_report, province, city, operators,
                                count, name)
                    }
                }
            }
            t.text = t.text.trim { it <= ' ' }.replace(" +".toRegex(), " ")
            if (t.text.isEmpty() || t.text.contains(
                            Resource.resources.getString(R.string.baidu_advertising))) {
                t.text = context.getString(R.string.unknown)
            }
            return t
        }
    }
}