package org.coolreader;


import com.youdao.sdk.app.YouDaoApplication;

import org.coolreader.crengine.TranslateUtil;

import cn.cyrus.translater.base.BaseApplication;

public class ReaderApplication extends BaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        YouDaoApplication.init(this, TranslateUtil.APP_KEY);

    }


}
