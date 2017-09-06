package com.fish.downloader.service

import android.app.Service
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.util.Log
import com.fish.downloader.IDownloadCK
import com.fish.downloader.IDownloader
import com.fish.downloader.framework.ThreadPool
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by fish on 17-9-6.
 */
class DownloadService : Service() {
    companion object {
        val DOWNLOAD_DIR = Environment.getExternalStoragePublicDirectory("fish/download")
        val TAG = "FISH DOWNLOAD SERVICE"
    }

    val mDownloaderBinder: IBinder = object : IDownloader.Stub() {
        override fun basicTypes(anInt: Int, aLong: Long, aBoolean: Boolean, aFloat: Float, aDouble: Double, aString: String?, aCK: IBinder?) {
        }

        override fun getAbsFilePath(tag: String): String? {
            return mDownloadMapper.get(tag)?.filePath
        }

        override fun startDownload(url: String, tag: String, fileName: String, fileSize: Long) {
            Log.e(TAG, "start dl: $url")
            mDownloadMapper.put(tag, Downloader.createDownloadInfo(url, tag, fileName, fileSize))
            ThreadPool.addTask { Downloader().get(mDownloadMapper.get(tag)?:return@addTask, mDownloadCKSender) }
        }

        override fun cancelDownloadByTag(tag: String?) {
            mDownloadMapper.get(tag)?.cancelSignal = true
        }

        override fun cancelAll() {
            mDownloadMapper.map { it.value?.cancelSignal = true }
        }

        override fun registerCB(ck: IBinder?) {
            mCKs.add(ck as? IDownloadCK ?: return)
        }

        override fun unregisterCB(ck: IBinder?) {
            mCKs.remove(ck as? IDownloadCK ?: return)
        }
    }

    val mDownloadCKSender = object : IDownloadCK.Stub() {
        override fun basicTypes(anInt: Int, aLong: Long, aBoolean: Boolean, aFloat: Float, aDouble: Double, aString: String?) {
        }

        override fun onProgress(tag: String?, pg: Double) {
            mCKs.map { it.onProgress(tag, pg)}
        }

        override fun onComplete(tag: String?, filePath: String?) {
            mCKs.map { it.onComplete(tag, filePath) }
            mDownloadMapper.remove(tag)
        }

        override fun onFailed(tag: String?, msg: String?) {
            mCKs.map { it.onFailed(tag, msg) }
            mDownloadMapper.remove(tag)
        }

        override fun onCanceled(tag: String?) {
            mCKs.map { it.onCanceled(tag) }
            mDownloadMapper.remove(tag)

        }
    }

    val mDownloadMapper = HashMap<String, DownloadInfo?>()
    val mCKs = ArrayList<IDownloadCK>()

    override fun onCreate() {
        Log.e(TAG, "ON CREATE")
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "ON START COMMAND")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.e(TAG, "ON BIND")
        return mDownloaderBinder
    }

}


data class DownloadInfo(val tag: String, val url: String, val fileName: String, var filePath: String, var fileSize: Long, var offset: Long, var cancelSignal: Boolean) : Serializable


class Downloader {
    companion object {
        private val BUF_SIZE = 256 * 1024
        fun createDownloadInfo(url: String, tag: String, fileName: String, fileSize: Long)
                = DownloadInfo(tag, url, fileName, "", fileSize, 0, false)
    }

    private fun createFile(info: DownloadInfo) = File(DownloadService.DOWNLOAD_DIR, "${info.fileName}").apply {
        if (!parentFile.exists()) parentFile.mkdirs()
        if (exists()) delete()
        createNewFile()
    }

    fun get(info: DownloadInfo, ck: IDownloadCK) = {
        try {
            val connection = URL(info.url).openConnection() as HttpURLConnection
            if (connection.responseCode == 200) {
                if (connection.contentLength != 0) info.fileSize = connection.contentLength.toLong()
                val f = createFile(info)
                info.filePath = f.absolutePath
                val fos = FileOutputStream(f)
                val netIS = connection.inputStream
                var downloadPtr = 0
                var readCnt = 0
                val buf = ByteArray(BUF_SIZE)
                do {
                    readCnt = netIS.read(buf, downloadPtr, BUF_SIZE)
                    fos.write(buf)
                    fos.flush()
                    downloadPtr += readCnt
                    ck.onProgress(info.tag, downloadPtr * 1.0 / info.fileSize)
                } while (readCnt != 0 && !info.cancelSignal)
                try {
                    fos.close()
                    netIS.close()
                    connection.disconnect()
                } catch (ioex: IOException) {
                    ioex.printStackTrace()
                }
                if (info.cancelSignal) {
                    ck.onCanceled(info.tag)
                    f.delete()
                } else ck.onComplete(info.tag, info.filePath)
            } else {
                ck.onFailed(info.tag, "REQUEST ERROR:${connection.responseCode}")
            }
        } catch (ioEX: IOException) {
            ioEX.printStackTrace()
            ck.onFailed(info.tag, "CONNECTION FAILED")
        }
    }
}
