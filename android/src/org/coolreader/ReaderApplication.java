package org.coolreader;

import android.app.Application;

import com.youdao.sdk.app.YouDaoApplication;

import org.coolreader.crengine.TranslateUtil;

public class ReaderApplication extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        YouDaoApplication.init(this, TranslateUtil.APP_KEY);
    }
}
