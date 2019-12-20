package cn

import cn.cyrus.translater.base.BaseApplication
import com.youdao.sdk.app.YouDaoApplication
import org.coolreader.crengine.Properties
import org.coolreader.crengine.SettingsManager

class ReaderApplication:BaseApplication(){
    override fun onCreate() {
        super.onCreate()
        YouDaoApplication.init(this, org.coolreader.crengine.TranslateUtil.APP_KEY)
        mSettingsManager = SettingsManager.getInstance(this)
    }
    fun settings(): Properties? {
        return mSettingsManager!!.mSettings
    }

     var mSettingsManager: SettingsManager? = null

}