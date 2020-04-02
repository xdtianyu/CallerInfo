package org.xdty.callerinfo.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object Toasts {
    private val handler = Handler(Looper.getMainLooper())
    private val map: HashMap<Toast, Long> = HashMap()
    private const val DURATION = 4000L

    fun show(context: Context, msg: Int) {
        show(context, context.getText(msg))
    }

    fun show(context: Context, msg: CharSequence) {
        handler.post {
            checkExpires()
            val toast = Toast.makeText(context, msg, Toast.LENGTH_LONG)
            map[toast] = System.currentTimeMillis()
            val delay = DURATION * (map.size - 1)
            handler.postDelayed({ toast.show() }, delay)
        }
    }

    private fun checkExpires() {
        with(map.entries.iterator()) {
            forEach {
                if (it.value < System.currentTimeMillis() - DURATION) {
                    remove()
                }
            }
        }
    }
}