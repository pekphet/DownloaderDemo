// IDownloader.aidl
package com.fish.fishdownloader;

// Declare any non-default types here with import statements

interface IDownloader {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString, IBinder aCK);
    String getAbsFilePath(String tag);
    void startDownload(String url, String tag, String fileName, long fileSize);
    void cancelDownloadByTag(String tag);
    void cancelAll();
    void registerCB(IBinder ck);
    void unregisterCB(IBinder ck);

}
