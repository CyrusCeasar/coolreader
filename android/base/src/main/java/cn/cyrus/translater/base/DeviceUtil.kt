package cn.cyrus.translater.base

import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.res.Resources
import android.text.ClipboardManager

fun dp2px(dp:Int):Int{
   return dp.px
}
fun copyToClipboard(ctx:Context, text: String?) {
    if (text != null && text.length > 0) {
        val cm: ClipboardManager = ctx.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.text = text
    }
}
val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()