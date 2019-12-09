package cn.cyrus.translater.base

import android.content.res.Resources

fun dp2px(dp:Int):Int{
   return dp.px
}

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()