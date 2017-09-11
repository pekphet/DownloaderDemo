package com.fish.downloader.service

import android.app.Service
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.util.Log
import com.fish.downloader.framework.ThreadPool
import com.fish.fishdownloader.IDownloadCK
import com.fish.fishdownloader.IDownloader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

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
            Log.e(TAG, "start download")
            mDownloadMapper.put(tag, Downloader.createDownloadInfo(url, tag, fileName, fileSize))
            ThreadPool.addTask(Downloader().get(mDownloadMapper.get(tag) ?: return, mDownloadCKSender))
        }

        override fun cancelDownloadByTag(tag: String?) {
            mDownloadMapper.get(tag)?.cancelSignal = true
        }

        override fun cancelAll() {
            mDownloadMapper.map { it.value?.cancelSignal = true }
        }

        override fun registerCB(ck: IBinder?) {
            Log.e(TAG, "reg")
            Log.e(TAG, "CKS LEN:${mCKs.size}")
            mCKs.add(IDownloadCK.Stub.asInterface(ck))
            Log.e(TAG, "CKS LEN:${mCKs.size}")
        }

        override fun unregisterCB(ck: IBinder?) {
            mCKs.remove(IDownloadCK.Stub.asInterface(ck))
        }
    }

    val mDownloadCKSender = object : IDownloadCK.Stub() {
        override fun basicTypes(anInt: Int, aLong: Long, aBoolean: Boolean, aFloat: Float, aDouble: Double, aString: String?) {
        }

        override fun onProgress(tag: String?, pg: Double) {
//            Log.e(TAG, "pg:$pg")
            mCKs.map { it.onProgress(tag, pg) }
        }

        override fun onComplete(tag: String?, filePath: String?) {
            Log.e(TAG, "complete:$filePath")
            mCKs.map { it.onComplete(tag, filePath) }
            mDownloadMapper.remove(tag)
        }

        override fun onFailed(tag: String?, msg: String?) {
            Log.e(TAG, "failed:$msg")
            mCKs.map { it.onFailed(tag, msg) }
            mDownloadMapper.remove(tag)
        }

        override fun onCanceled(tag: String?) {
            Log.e(TAG, "Cancel:$tag")
            mCKs.map { it.onCanceled(tag) }
            mDownloadMapper.remove(tag)

        }
    }

    val mDownloadMapper = HashMap<String, DownloadInfo?>()
    val mCKs = ArrayList<IDownloadCK>()

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        private val BUF_SIZE = 2 * 1024
        val TAG = "fish downloader"
        fun createDownloadInfo(url: String, tag: String, fileName: String, fileSize: Long)
                = DownloadInfo(tag, url, fileName, "", fileSize, 0, false)
    }

    private fun createFile(info: DownloadInfo) = File(DownloadService.DOWNLOAD_DIR, "${info.fileName}").apply {
        Log.e(TAG, "CREATE FILE")
        if (!parentFile.exists()) parentFile.mkdirs()
        if (exists()) delete()
        createNewFile()
    }

    fun get(info: DownloadInfo, ck: IDownloadCK) = Runnable {
        try {
            val connection = URL(info.url).openConnection() as HttpURLConnection
            Log.e(TAG, "url connected!")
            if (connection.responseCode == 200) {
                Log.e(TAG, "code:${connection.responseCode}")
                if (connection.contentLength != 0) info.fileSize = connection.contentLength.toLong()
                Log.e(TAG, "lenth:${info.fileSize}")
                val f = createFile(info)
                info.filePath = f.absolutePath
                val fos = FileOutputStream(f)
                val netIS = connection.inputStream
                var downloadPtr = 0
                var readCnt = 0
                val buf = ByteArray(BUF_SIZE)
                do {
                    readCnt = netIS.read(buf, 0, BUF_SIZE)
                    if (readCnt == -1)
                        break
                    fos.write(buf, 0, readCnt)
                    fos.flush()
                    Log.e(TAG, "dptr:$downloadPtr, readCnt:$readCnt, BUF SIZE: $BUF_SIZE")
                    downloadPtr += readCnt
                    ck.onProgress(info.tag, downloadPtr * 1.0 / info.fileSize)
                } while (readCnt > 0 && !info.cancelSignal)
                Log.e(TAG, "exit looper")
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
