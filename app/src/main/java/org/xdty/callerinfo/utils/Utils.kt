package org.xdty.callerinfo.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings.Secure
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.Log
import androidx.core.app.NotificationCompat.Builder
import org.xdty.callerinfo.R
import org.xdty.callerinfo.activity.MarkActivity
import org.xdty.callerinfo.model.MarkType
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.model.setting.SettingImpl
import org.xdty.phone.number.model.Type
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Utils private constructor() {

    companion object {
        private const val NOTIFICATION_MARK = 0x01
        private val TAG = Utils::class.java.simpleName
        private var sNumberSourceMap: MutableMap<Int, String>? = null
        fun getDate(timestamp: Long): String {
            val calendar = Calendar.getInstance()
            val tz = TimeZone.getDefault()
            calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.timeInMillis))
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val currentTimeZone = Date(timestamp)
            return sdf.format(currentTimeZone)
        }

        fun getTime(timestamp: Long): String {
            val calendar = Calendar.getInstance()
            val tz = TimeZone.getDefault()
            calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.timeInMillis))
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
            val currentTimeZone = Date(timestamp)
            return sdf.format(currentTimeZone)
        }

        fun readableDate(timestamp: Long): String {
            val current = System.currentTimeMillis()
            return DateUtils.getRelativeTimeSpanString(timestamp, current, DateUtils.DAY_IN_MILLIS)
                    .toString()
        }

        fun readableTime(duration: Long): String {
            val result: String
            val seconds = (duration / 1000).toInt() % 60
            val minutes = (duration / (1000 * 60) % 60).toInt()
            val hours = (duration / (1000 * 60 * 60) % 24).toInt()
            result = if (duration < 60000) {
                Resource.resources
                        .getString(R.string.readable_second, seconds)
            } else if (duration < 3600000) {
                Resource.resources
                        .getString(R.string.readable_minute, minutes, seconds)
            } else {
                Resource.resources
                        .getString(R.string.readable_hour, hours, minutes, seconds)
            }
            return result
        }

        fun mask(s: String): String {
            return s.replace("([0-9]|[a-f])".toRegex(), "*")
        }

        fun changeLang(context: Context): ContextWrapper {
            var context = context
            if (!SettingImpl.instance.isForceChinese) {
                return ContextWrapper(context)
            }
            val rs = context.resources
            val config = rs.configuration
            val langCode = "zh"
            val locale = Locale(langCode)
            Locale.setDefault(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale)
            } else {
                config.locale = locale
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                context = context.createConfigurationContext(config)
            } else {
                context.resources
                        .updateConfiguration(config, context.resources.displayMetrics)
            }
            return ContextWrapper(context)
        }

        fun isAppInstalled(context: Context, packageName: String?): Boolean {
            return try {
                context.packageManager.getApplicationInfo(packageName, 0)
                true
            } catch (e: NameNotFoundException) {
                false
            }
        }

        fun getVersionCode(context: Context, packageName: String?): Int {
            return try {
                val info = context.packageManager.getPackageInfo(packageName, 0)
                info.versionCode
            } catch (e: NameNotFoundException) {
                0
            }
        }

        fun bundleToString(bundle: Bundle?): String {
            val out = StringBuilder("Bundle[")
            if (bundle == null) {
                out.append("null")
            } else {
                var first = true
                for (key in bundle.keySet()) {
                    if (!first) {
                        out.append(", ")
                    }
                    out.append(key).append('=')
                    val value = bundle[key]
                    if (value is IntArray) {
                        out.append(Arrays.toString(value as IntArray?))
                    } else if (value is ByteArray) {
                        out.append(Arrays.toString(value as ByteArray?))
                    } else if (value is BooleanArray) {
                        out.append(Arrays.toString(value as BooleanArray?))
                    } else if (value is ShortArray) {
                        out.append(Arrays.toString(value as ShortArray?))
                    } else if (value is LongArray) {
                        out.append(Arrays.toString(value as LongArray?))
                    } else if (value is FloatArray) {
                        out.append(Arrays.toString(value as FloatArray?))
                    } else if (value is DoubleArray) {
                        out.append(Arrays.toString(value as DoubleArray?))
                    } else if (value is Array<*>) {
                        out.append(value.contentToString())
                    } else if (value is Bundle) {
                        out.append(bundleToString(value as Bundle?))
                    } else {
                        out.append(value)
                    }
                    first = false
                }
            }
            out.append("]")
            return out.toString()
        }

        fun getDeviceId(context: Context): String {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val tmDevice: String
            val tmSerial: String
            tmDevice = "" + tm.deviceId
            tmSerial = "" + tm.simSerialNumber
            val androidId: String = "" + Secure.getString(context.contentResolver, Secure.ANDROID_ID)
            val deviceUuid = UUID(androidId.hashCode().toLong(),
                    tmDevice.hashCode().toLong() shl 32 or tmSerial.hashCode().toLong())
            return deviceUuid.toString()
        }

        fun showMarkNotification(context: Context, number: String?) {
            val intent = Intent(context, MarkActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val setting: Setting = SettingImpl.instance
            setting.addPaddingMark(number!!)
            val list: ArrayList<String> = setting.paddingMarks
            val numbers = TextUtils.join(", ", list)
            val manager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE) as NotificationManager
            val requestCode = Random().nextInt()
            val pIntent = PendingIntent.getActivity(context, requestCode, intent, 0)
            val builder = Builder(context)
                    .setSmallIcon(R.drawable.status_icon)
                    .setContentIntent(pIntent)
                    .setContentTitle(context.getString(R.string.mark_number))
                    .setContentText(numbers)
                    .setAutoCancel(true)
                    .setContentIntent(pIntent)
            manager.notify(NOTIFICATION_MARK, builder.build())
        }

        fun startMarkActivity(context: Context, number: String?) {
            val intent = Intent(context, MarkActivity::class.java)
            intent.putExtra(MarkActivity.NUMBER, number)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        fun typeFromString(type: String): Int {
            if (TextUtils.isEmpty(type)) {
                return -1
            }
            val types = ArrayList(
                    Arrays.asList(*Resource.resources
                            .getStringArray(R.array.mark_type_source)))
            for (t in types) {
                if (t.contains(type)) {
                    return types.indexOf(t)
                }
                val ts = ArrayList(Arrays.asList(*t.split("\\|").toTypedArray()))
                for (s in ts) {
                    if (type.contains(s!!)) {
                        return types.indexOf(t)
                    }
                }
            }
            Log.e(TAG, "typeFromString failed: $type")
            return -1
        }

        fun sourceFromId(sourceId: Int): String? {
            if (sourceId == -9999) {
                return Resource.resources
                        .getString(R.string.mark)
            }
            if (sNumberSourceMap == null) {
                sNumberSourceMap = HashMap()
                val values = Resource.resources.getStringArray(R.array.source_values)
                val keys = Resource.resources
                        .getIntArray(R.array.source_keys)
                for (i in keys.indices) {
                    sNumberSourceMap!![keys[i]] = values[i]
                }
            }
            return sNumberSourceMap!![sourceId]
        }

        fun typeFromId(type: Int): String {
            val values = Resource.resources.getStringArray(R.array.mark_type)
            return if (type >= 0 && type < values.size) {
                values[type]
            } else {
                Resource.resources.getString(R.string.custom)
            }
        }

        fun markTypeFromName(name: String): Type {
            val type = typeFromString(name)
            return when (MarkType.fromInt(type)) {
                MarkType.HARASSMENT, MarkType.FRAUD, MarkType.ADVERTISING -> Type.REPORT
                MarkType.EXPRESS_DELIVERY, MarkType.RESTAURANT_DELIVER -> Type.POI
                else -> Type.POI
            }
        }

        fun isComponentEnabled(pm: PackageManager, pkgName: String?, clsName: String): Boolean {
            val componentName = ComponentName(pkgName!!, clsName)
            val componentEnabledSetting = pm.getComponentEnabledSetting(componentName)
            return when (componentEnabledSetting) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ->  // We need to get the application info to get the component's default state
                    try {
                        val packageInfo = pm.getPackageInfo(pkgName,
                                PackageManager.GET_ACTIVITIES
                                        or PackageManager.GET_RECEIVERS
                                        or PackageManager.GET_SERVICES
                                        or PackageManager.GET_PROVIDERS
                                        or PackageManager.GET_DISABLED_COMPONENTS)
                        val components: ArrayList<ComponentInfo> = ArrayList()
                        if (packageInfo.activities != null) {
                            Collections.addAll(components, *packageInfo.activities)
                        }
                        if (packageInfo.services != null) {
                            Collections.addAll(components, *packageInfo.services)
                        }
                        if (packageInfo.providers != null) {
                            Collections.addAll(components, *packageInfo.providers)
                        }
                        for (componentInfo in components) {
                            if (componentInfo.name == clsName) {
                                return componentInfo.isEnabled
                            }
                        }
                        // the component is not declared in the AndroidManifest
                        false
                    } catch (e: NameNotFoundException) { // the package isn't installed on the device
                        false
                    }
                else -> try {
                    val packageInfo = pm.getPackageInfo(pkgName,
                            PackageManager.GET_ACTIVITIES
                                    or PackageManager.GET_RECEIVERS
                                    or PackageManager.GET_SERVICES
                                    or PackageManager.GET_PROVIDERS
                                    or PackageManager.GET_DISABLED_COMPONENTS)
                    val components: ArrayList<ComponentInfo> = ArrayList()
                    if (packageInfo.activities != null) {
                        Collections.addAll(components, *packageInfo.activities)
                    }
                    if (packageInfo.services != null) {
                        Collections.addAll(components, *packageInfo.services)
                    }
                    if (packageInfo.providers != null) {
                        Collections.addAll(components, *packageInfo.providers)
                    }
                    for (componentInfo in components) {
                        if (componentInfo.name == clsName) {
                            return componentInfo.isEnabled
                        }
                    }
                    false
                } catch (e: NameNotFoundException) {
                    false
                }
            }
        }

        fun ignoreBatteryOptimization(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val powerManager = context.getSystemService(
                            Context.POWER_SERVICE) as PowerManager
                    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return true
        }
    }

    init {
        throw AssertionError("Utils class is not meant to be instantiated or subclassed.")
    }
}