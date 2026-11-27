package com.library.visitorsystem

import android.webkit.JavascriptInterface

class NativeBridge {

    companion object {
        init {
            System.loadLibrary("library_native")
        }
    }

    private external fun nativeCalculateOverdueFine(daysOverdue: Long, dailyRate: Double): Double
    private external fun nativeGetLibraryEngineStatus(): String

    @JavascriptInterface
    fun calculateOverdueFine(daysOverdue: Long, dailyRate: Double): Double {
        return nativeCalculateOverdueFine(daysOverdue, dailyRate)
    }

    @JavascriptInterface
    fun getLibraryEngineStatus(): String {
        return nativeGetLibraryEngineStatus()
    }
}