// IDownloadCK.aidl
package com.fish.fishdownloader;

// Declare any non-default types here with import statements

interface IDownloadCK {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString);

    void onProgress(String tag, double pg);
    void onComplete(String tag, String filePath);
    void onFailed(String tag, String msg);
    void onCanceled(String tag);

}
