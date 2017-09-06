package com.fish.downloader.view

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.fish.downloader.IDownloadCK
import com.fish.downloader.IDownloader
import com.fish.downloader.R
import com.fish.downloader.extensions.bid
import com.fish.downloader.service.DownloadService

/**
 * Created by fish on 17-9-6.
 */
class DownloadBar(ctx: Context, attrs: AttributeSet?, defSA: Int, defRes: Int) : FrameLayout(ctx, attrs, defSA, defRes) {
    constructor(ctx: Context) : this(ctx, null)
    constructor(ctx: Context, attrs: AttributeSet?) : this(ctx, attrs, 0, 0)
    constructor(ctx: Context, attrs: AttributeSet?, defSA: Int) : this(ctx, attrs, defSA, 0)

    val DOWNLOADING_COLOR = 0xFF26D054
    val COMPLETE_COLOR = 0xFF5AA3E0

    init {
        View.inflate(context, R.layout.v_download_bar, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initView()
    }

    private fun initView() {
        mTvPG.text = mInitText
        mImgPG.setBackgroundColor(DOWNLOADING_COLOR.toInt())
        Log.e("attach to win", "att")
    }

    val mTvPG by bid<TextView>(R.id.tv_pg)
    val mImgPG by bid<ImageView>(R.id.img_progress)
    var mServiceBinder: IDownloader? = null

    var mInitText: String = "点击下载"
    var mCompleteText: String = "下载完成"

    fun setCompleteText(text: String) {
        mCompleteText = text
    }

    fun setInitText(text: String) {
        mInitText = text
    }

    fun download(url: String, tag: String, fileName: String, fileSize: Long, dlck: (type: CK_TYPE, data: String?) -> Unit) {
        val ck = object : IDownloadCK.Stub() {
            override fun basicTypes(anInt: Int, aLong: Long, aBoolean: Boolean, aFloat: Float, aDouble: Double, aString: String?) {}

            override fun onProgress(tag2: String?, pg: Double) {
                if (tag2.equals(tag))
                    progress(pg)
            }

            override fun onComplete(tag2: String?, filePath: String?) {
                if (tag2.equals(tag)) {
                    complete(filePath)
                    dlck(CK_TYPE.COMPLETE, filePath)
                }
            }

            override fun onFailed(tag2: String?, msg: String?) {
                if (tag2.equals(tag)) {
                    dlck(CK_TYPE.FAILED, msg)
                }
            }

            override fun onCanceled(tag2: String?) {
                if (tag2.equals(tag)) {
                    dlck(CK_TYPE.CANCELED, "")
                }
            }
        }
        context.bindService(Intent(context, DownloadService::class.java), object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                Log.e("remote service", "disconnected")
            }

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.e("remote service", "connected")
                mServiceBinder = IDownloader.Stub.asInterface(service)
                mServiceBinder?.registerCB(ck)
                mServiceBinder?.startDownload(url, tag, fileName, fileSize)
            }
        }, Service.BIND_AUTO_CREATE)
    }

    private fun complete(filePath: String?) {
        mImgPG.setBackgroundColor(COMPLETE_COLOR.toInt())
    }

    private fun progress(pg: Double) {
        mTvPG.text = String.format("下载中  %.2f%%", pg)
        mImgPG.layoutParams = mTvPG.layoutParams.apply { width = (width * pg).toInt() }
    }

    enum class CK_TYPE {COMPLETE, CANCELED, FAILED }
}