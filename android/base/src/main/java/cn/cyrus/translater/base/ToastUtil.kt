package cn.cyrus.translater.base

import android.content.Context
import android.widget.Toast


fun showToast(ctx:Context,msg:String,len:Int = Toast.LENGTH_SHORT){
    Toast.makeText(ctx.applicationContext,msg,len).show()
}