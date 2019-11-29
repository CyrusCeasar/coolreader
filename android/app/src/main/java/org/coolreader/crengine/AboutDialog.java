package org.coolreader.crengine;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

import org.coolreader.CoolReaderActivity;
import org.coolreader.R;

import java.util.Random;

public class AboutDialog extends BaseDialog implements TabContentFactory {
    final CoolReaderActivity mCoolReader;

    private View mAppTab;
    private View mLicenseTab;


    public AboutDialog(CoolReaderActivity activity) {
        super(activity);
        mCoolReader = activity;
        setTitle(R.string.dlg_about);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        TabHost tabs = (TabHost) inflater.inflate(R.layout.about_dialog, null);
        mAppTab = inflater.inflate(R.layout.about_dialog_app, null);
        ((TextView) mAppTab.findViewById(R.id.version)).setText("Cool Reader " + mCoolReader.getVersion());
        mLicenseTab = inflater.inflate(R.layout.about_dialog_license, null);
        String license = Engine.getInstance(mCoolReader).loadResourceUtf8(R.raw.license);
        ((TextView) mLicenseTab.findViewById(R.id.license)).setText(license);


        tabs.setup();
        TabHost.TabSpec tsApp = tabs.newTabSpec("App");
        tsApp.setIndicator("",
                getContext().getResources().getDrawable(R.drawable.cr3_menu_link));
        tsApp.setContent(this);
        tabs.addTab(tsApp);

        TabHost.TabSpec tsLicense = tabs.newTabSpec("License");
        tsLicense.setIndicator("",
                getContext().getResources().getDrawable(R.drawable.ic_menu_star));
        tsLicense.setContent(this);
        tabs.addTab(tsLicense);

        setView(tabs);


    }

    private static Random rnd = new Random(android.os.SystemClock.uptimeMillis());


    @Override
    public View createTabContent(String tag) {
        if ("App".equals(tag))
            return mAppTab;
        else if ("License".equals(tag))
            return mLicenseTab;

        return null;
    }

}
