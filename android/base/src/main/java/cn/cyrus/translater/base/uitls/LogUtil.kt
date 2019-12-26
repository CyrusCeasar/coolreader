package cn.cyrus.translater.base.uitls

import android.util.Log
import cn.cyrus.translater.BuildConfig


/**
 * Created by ChenLei on 2018/8/20 0020.
 */


const val TAG = BuildConfig.LIBRARY_PACKAGE_NAME

fun logi(content: String, tag: String = TAG) {
    Log.i(tag, content)
}

fun logd(content: String, tag: String = TAG) {
    Log.d(tag, content)
}


fun loge(content: String, tag: String = TAG) {
    Log.e(tag,content)
}


fun logw(content: String, tag: String = TAG) {
    Log.w(tag,content)
}






