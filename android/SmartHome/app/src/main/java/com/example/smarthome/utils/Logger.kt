package com.example.smarthome.utils

import android.util.Log

object Logger {
    fun debug(tag: String, message: String) = Log.d(tag, message)
    fun error(tag: String, message: String) = Log.e(tag, message)
} 