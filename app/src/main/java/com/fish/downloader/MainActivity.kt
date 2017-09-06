package com.fish.downloader

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.fish.downloader.extensions.bid
import com.fish.downloader.view.DownloadBar

class MainActivity : AppCompatActivity() {

    val mDnBar by bid<DownloadBar>(R.id.vdb)
    var enableDownload = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mDnBar.setOnClickListener {
            if (!enableDownload) return@setOnClickListener
            enableDownload = false
            mDnBar.download("http://45.78.46.200:42002/docker_practice.pdf", "docker",
                    "docker.pdf", 123L) { type, msg ->
                run {
                    when (type) {
                        DownloadBar.CK_TYPE.COMPLETE -> Tst("下载成功：${msg}")
                        DownloadBar.CK_TYPE.FAILED -> Tst("下载失败：${msg}")
                        DownloadBar.CK_TYPE.CANCELED -> Tst("下载取消：${msg}")
                    }
                    enableDownload = false
                }
            }
        }
    }

}

fun Activity.Tst(msg: String) = Toast.makeText(this@Tst, msg, Toast.LENGTH_SHORT).show()