package cn.cyrus.translater.base.uitls

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper

object Dispatch {
    fun asyncOnBackground(call: ()->Unit) {
        AsyncTask.execute {
            call()
        }
    }

    fun asyncOnMain(call: ()->Unit) {
        Handler(Looper.getMainLooper()).post {
            call()
        }
    }
}