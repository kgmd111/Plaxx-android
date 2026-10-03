package com.plaxx.app.ui.debug

import android.util.Log

object PerfLog {
    const val TAG = "PLAXX_PERF"
    var enabled = true

    fun d(msg: String) { if (enabled) Log.d(TAG, msg) }
    fun w(msg: String) { if (enabled) Log.w(TAG, msg) }
    fun e(msg: String) { if (enabled) Log.e(TAG, msg) }
}
