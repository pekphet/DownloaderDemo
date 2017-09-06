package com.fish.downloader.extensions

import android.app.Activity
import android.view.View

/**
 * Created by fish on 17-9-6.
 */
fun <T : View> Activity.bid(id: Int) = lazy { findViewById(id) as T}
fun <T : View> View.bid(id: Int) = lazy { findViewById(id) as T}