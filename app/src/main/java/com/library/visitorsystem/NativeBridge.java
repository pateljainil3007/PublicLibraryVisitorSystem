package com.library.visitorsystem;

import android.webkit.JavascriptInterface;

public class NativeBridge {

    static {
        System.loadLibrary("library_native");
    }

    private native double nativeCalculateOverdueFine(long daysOverdue, double dailyRate);

    private native String nativeGetLibraryEngineStatus();

    @JavascriptInterface
    public double calculateOverdueFine(long daysOverdue, double dailyRate) {
        return nativeCalculateOverdueFine(daysOverdue, dailyRate);
    }

    @JavascriptInterface
    public String getLibraryEngineStatus() {
        return nativeGetLibraryEngineStatus();
    }
}