package cn.cyrus.translater.base.uitls

import android.content.Context
import android.os.Environment
import android.os.Environment.isExternalStorageRemovable
import android.provider.Settings.Secure
import java.io.File


/**
 * Created by ChenLei on 2018/8/23 0023.
 */
fun getDiskCacheDir(context: Context, uniqueName: String): File {
    val b1 = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
    var b2 = !isExternalStorageRemovable()
    val cachePath: String = if (b1 || b2) {
        context.externalCacheDir!!.path
    } else {context.cacheDir.path
    }
    return File(cachePath + File.separator + uniqueName)
}

fun getDeviceId(context:Context):String{
   return Secure.getString(context.contentResolver, Secure.ANDROID_ID)
}