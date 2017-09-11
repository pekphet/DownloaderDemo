package com.fish.downloader.view

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.fish.downloader.extensions.bid
import com.fish.downloader.service.DownloadService
import com.fish.fishdownloader.IDownloadCK
import com.fish.fishdownloader.IDownloader
import com.fish.fishdownloader.R

/**
 * Created by fish on 17-9-6.
 */
class DownloadBar(ctx: Context, attrs: AttributeSet?, defSA: Int, defRes: Int) : FrameLayout(ctx, attrs, defSA, defRes) {
    constructor(ctx: Context) : this(ctx, null)
    constructor(ctx: Context, attrs: AttributeSet?) : this(ctx, attrs, 0, 0)
    constructor(ctx: Context, attrs: AttributeSet?, defSA: Int) : this(ctx, attrs, defSA, 0)

    companion object {
        val DOWNLOADING_COLOR: Int = 0xFF26D054.toInt()
        val COMPLETE_COLOR: Int = 0xFF5AA3E0.toInt()
    }

    init {
        View.inflate(context, R.layout.v_download_bar, this)
    }

    val mTvPG by bid<TextView>(R.id.tv_dlbar_pg)
    val mFlPG by bid<FrameLayout>(R.id.fl_dlbar_progress)
    val mBG by bid<FrameLayout>(R.id.fl_dlbar_bg)
    val mMask by bid<ImageView>(R.id.img_dlbar_mask)

    var mConf = DownloadBarConfigure()
        set(conf) {
            field = conf
            initView()
        }


    var mServiceBinder: IDownloader? = null

    val mHandler = Handler(Looper.getMainLooper())

    var mConnection: ServiceConnection? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initView()
    }

    private fun initView() {
        mTvPG.text = mConf.initText
        mMask.setBackgroundResource(mConf.maskRes)
        if (mConf.baseBGRes == null) mBG.setBackgroundColor(mConf.baseBGColor) else mBG.setBackgroundResource(mConf.baseBGRes ?: return)
        if (mConf.initBGRes == null) mFlPG.setBackgroundColor(mConf.initBGColor) else mFlPG.setBackgroundResource(mConf.initBGRes ?: return)
        Log.e("attach to win", "att")
    }

    fun download(url: String, tag: String, fileName: String, fileSize: Long, dlck: (type: CK_TYPE, data: String?) -> Unit) {
        chgDownloadUI()
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
        mConnection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                Log.e("remote service", "disconnected")
            }

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.e("remote service", "connected")
                mServiceBinder = IDownloader.Stub.asInterface(service)
                mServiceBinder?.registerCB(ck)
                mServiceBinder?.startDownload(url, tag, fileName, fileSize)
            }
        }
        context.bindService(Intent(context, DownloadService::class.java), mConnection, Service.BIND_AUTO_CREATE)
    }

    private fun chgDownloadUI() {
        if (mConf.downloadingBGRes == null) mFlPG.setBackgroundColor(mConf.downloadingBGColor) else mFlPG.setBackgroundResource(mConf.downloadingBGRes ?: return)
    }

    private fun complete(filePath: String?) {
        mHandler.post {
            if (mConf.completeBGRes == null) mFlPG.setBackgroundColor(mConf.completeBGColor) else mFlPG.setBackgroundResource(mConf.completeBGRes ?: return@post)
            mTvPG.text = mConf.completeText
        }
    }

    private fun progress(pg: Double) {
        mHandler.post {
            mTvPG.text = String.format(mConf.downloadingText, pg * 100)
            mConf.pogressCK(this@DownloadBar, mFlPG, pg)
        }
    }

    fun disconnectService() {
        context.unbindService(mConnection ?: return)
    }

    enum class CK_TYPE {COMPLETE, CANCELED, FAILED }

    data class DownloadBarConfigure(var initText: String = "开始下载", var downloadingText: String = "下载中  %.2f%%",
                                    var completeText: String = "下载完成",
                                    var initBGColor: Int = DOWNLOADING_COLOR, var initBGRes: Int? = null,
                                    var downloadingBGColor: Int = DownloadBar.DOWNLOADING_COLOR, var completeBGColor: Int = DownloadBar.COMPLETE_COLOR,
                                    var downloadingBGRes: Int? = null, var completeBGRes: Int? = null,
                                    var baseBGColor: Int = 0xffffffff.toInt(), var baseBGRes: Int? = null,
                                    var maskRes: Int = R.drawable.progress_top, var pogressCK: (parentView: View, progressBar: FrameLayout, pg: Double) -> Unit = { view, img, pg -> img.layoutParams = img.layoutParams.apply { this@apply.width = (view.width * pg).toInt() } })
}