package cn

import cn.cyrus.translater.base.BaseApplication
import cn.cyrus.translater.feater.TranslateUtil
import com.youdao.sdk.app.YouDaoApplication
import org.coolreader.crengine.Properties
import org.coolreader.crengine.SettingsManager

class ReaderApplication:BaseApplication(){
    override fun onCreate() {
        super.onCreate()
        YouDaoApplication.init(this, TranslateUtil.APP_KEY)
        mSettingsManager = SettingsManager.getInstance(this)
    }
    fun settings(): Properties? {
        return mSettingsManager!!.mSettings
    }

     var mSettingsManager: SettingsManager? = null

}