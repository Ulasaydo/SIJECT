package com.signlanguage.translator.utils

import android.util.Log

object LogUtils {
    private const val PREFIX = "SIJECT"

    fun d(tag: String, message: String) {
        Log.d("$PREFIX:$tag", message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$PREFIX:$tag", message, throwable)
    }
}
