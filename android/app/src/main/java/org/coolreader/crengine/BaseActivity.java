package org.coolreader.crengine;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.coolreader.R;
import org.coolreader.crengine.reader.ReaderView;
import org.coolreader.db.CRDBService;
import org.coolreader.db.CRDBServiceAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import cn.ReaderApplication;

public class BaseActivity extends AppCompatActivity implements Settings {

    private static final Logger log = L.create("ba");
    protected SettingsManager mSettingsManager = null;
    private CRDBServiceAccessor mCRDBService;

    protected void unbindCRDBService() {
        if (mCRDBService != null) {
            mCRDBService.unbind();
            mCRDBService = null;
        }
    }

    public void releaseBacklightControl() {
        backlightControl.release();
    }

    public ScreenBacklightControl backlightControl;

    protected void bindCRDBService() {
        if (mCRDBService == null) {
            mCRDBService = new CRDBServiceAccessor(this, Engine.getInstance(this).getPathCorrector());
        }
        mCRDBService.bind(null);
    }

    /**
     * Wait until database is bound.
     *
     * @param readyCallback to be called after DB is ready
     */
    public void waitForCRDBService(Runnable readyCallback) {
        if (mCRDBService == null) {
            mCRDBService = new CRDBServiceAccessor(this, Engine.getInstance(this).getPathCorrector());
        }
        mCRDBService.bind(readyCallback);
    }

    public CRDBService.LocalBinder getDB() {
        return mCRDBService != null ? mCRDBService.get() : null;
    }

    public Properties settings() {
        return mSettingsManager.mSettings;
    }


    protected void startServices() {
        // create settings
        // create rest of settings
        Services.startServices(this);
    }


    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log.i("BaseActivity.onCreate() entered");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSettingsManager = ((ReaderApplication) getApplication()).getMSettingsManager();

        super.onCreate(savedInstanceState);
        backlightControl = new ScreenBacklightControl(this);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            mVersion = pi.versionName;
        } catch (NameNotFoundException e) {
            // ignore
        }
        log.i("CoolReaderActivity version : " + getVersion());

        Display d = getWindowManager().getDefaultDisplay();
        DisplayMetrics m = new DisplayMetrics();
        d.getMetrics(m);
        try {
            Field fld = m.getClass().getField("densityDpi");
            if (fld != null) {
                Object v = fld.get(m);
                if (v != null && v instanceof Integer) {
                    densityDpi = ((Integer) v).intValue();
                    log.i("Screen density detected: " + densityDpi + "DPI");
                }
            }
        } catch (Exception e) {
            log.e("Cannot find field densityDpi, using default value");
        }
        float widthInches = m.widthPixels / densityDpi;
        float heightInches = m.heightPixels / densityDpi;
        diagonalInches = (float) Math.sqrt(widthInches * widthInches + heightInches * heightInches);

        log.i("diagonal=" + diagonalInches + "  isSmartphone=" + isSmartphone());
        //log.i("CoolReaderActivity.window=" + getWindow());
        if (!DeviceInfo.EINK_SCREEN) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.alpha = 1.0f;
            lp.dimAmount = 0.0f;
            if (!DeviceInfo.EINK_SCREEN)
                lp.format = DeviceInfo.PIXEL_FORMAT;
            lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
            lp.horizontalMargin = 0;
            lp.verticalMargin = 0;
            lp.windowAnimations = 0;
            lp.layoutAnimationParameters = null;
            lp.memoryType = WindowManager.LayoutParams.MEMORY_TYPE_NORMAL;
            getWindow().setAttributes(lp);
        }

        // load settings
        Properties props = mSettingsManager.mSettings;
        String theme = props.getProperty(ReaderView.PROP_APP_THEME, DeviceInfo.FORCE_LIGHT_THEME ? "WHITE" : "LIGHT");
        String lang = props.getProperty(ReaderView.PROP_APP_LOCALE, Lang.DEFAULT.code);
        setLanguage(lang);
        setCurrentTheme(theme);


        backlightControl.setScreenBacklightDuration(props.getInt(ReaderView.PROP_APP_SCREEN_BACKLIGHT_LOCK, 3));

        setFullscreen(props.getBool(ReaderView.PROP_APP_FULLSCREEN, (DeviceInfo.EINK_SCREEN ? true : false)));
        int orientation = props.getInt(ReaderView.PROP_APP_SCREEN_ORIENTATION, 5); //(DeviceInfo.EINK_SCREEN?0:4)
        if (orientation < 0 || orientation > 5)
            orientation = 5;
        setScreenOrientation(orientation);
        int backlight = props.getInt(ReaderView.PROP_APP_SCREEN_BACKLIGHT, -1);
        if (backlight < -1 || backlight > 100)
            backlight = -1;
        backlightControl.setScreenBacklightLevel(backlight);


        bindCRDBService();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindCRDBService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mIsStarted = true;
        mPaused = false;
        backlightControl.onUserActivity();
    }

    @Override
    protected void onStop() {
        mIsStarted = false;
        super.onStop();
    }

    @Override
    protected void onPause() {
        log.i("CoolReaderActivity.onPause() : saving reader state");
        mIsStarted = false;
        mPaused = true;
//		setScreenUpdateMode(-1, mReaderView);
        einkRefresh();
        releaseBacklightControl();
        super.onPause();
    }

    public void einkRefresh() {
        EinkScreen.Refresh();
    }


    public static String PREF_FILE = "CR3LastBook";
    public static String PREF_LAST_BOOK = "LastBook";
    public static String PREF_LAST_LOCATION = "LastLocation";
    public static String PREF_LAST_NOTIFICATION = "LastNoticeNumber";

    @Override
    protected void onResume() {
        log.i("CoolReaderActivity.onResume()");
        mPaused = false;
        mIsStarted = true;
        backlightControl.onUserActivity();
        super.onResume();
    }

    boolean mIsStarted = false;
    private boolean mPaused = false;

    public boolean isStarted() {
        return mIsStarted;
    }

    private String mVersion = "3.1";

    public String getVersion() {
        return mVersion;
    }


    public int getPalmTipPixels() {
        return densityDpi / 20; // 1/3"
    }

    public int getDensityDpi() {
        return densityDpi;
    }


    public boolean isSmartphone() {
        return diagonalInches <= 6.2; //5.8;
    }

    private int densityDpi = 160;
    private float diagonalInches = 4;


    private InterfaceTheme currentTheme = DeviceInfo.FORCE_LIGHT_THEME ? InterfaceTheme.WHITE : InterfaceTheme.LIGHT;

    public InterfaceTheme getCurrentTheme() {
        return currentTheme;
    }

    public void setCurrentTheme(String themeCode) {
        InterfaceTheme theme = InterfaceTheme.findByCode(themeCode);
        if (theme != null && currentTheme != theme) {
            setCurrentTheme(theme);
        }
    }

    private int preferredItemHeight = 36;

    public int getPreferredItemHeight() {
        return preferredItemHeight;
    }

    private int minFontSize = 9;

    public int getMinFontSize() {
        return minFontSize;
    }

    private int maxFontSize = 90;

    public int getMaxFontSize() {
        return maxFontSize;
    }

    public void updateBackground() {
        TypedArray a = getTheme().obtainStyledAttributes(new int[]{android.R.attr.windowBackground, android.R.attr.background, android.R.attr.textColor, android.R.attr.colorBackground, android.R.attr.colorForeground, android.R.attr.listPreferredItemHeight});
        int bgRes = a.getResourceId(0, 0);
        int clBackground = a.getColor(2, 0);
        preferredItemHeight = densityDpi / 3; //a.getDimensionPixelSize(5, 36);
        if (contentView != null) {
            if (bgRes != 0) {

                contentView.setBackgroundResource(bgRes);
            } else if (clBackground != 0) {
                contentView.setBackgroundColor(clBackground);
            }
        } else {

        }
        a.recycle();
        Display display = getWindowManager().getDefaultDisplay();
        int sz = display.getWidth();
        if (sz > display.getHeight())
            sz = display.getHeight();
        minFontSize = sz / 45;
        maxFontSize = sz / 8;
        if (maxFontSize > 340)
            maxFontSize = 340;
        if (minFontSize < 9)
            minFontSize = 9;
    }

    public void onUserActivity() {
        backlightControl.onUserActivity();

    }

    public void setCurrentTheme(InterfaceTheme theme) {
        log.i("setCurrentTheme(" + theme + ")");
        currentTheme = theme;
        getApplication().setTheme(theme.getThemeId());
        setTheme(theme.getThemeId());
        updateBackground();
    }

    int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_USER;

    public void applyScreenOrientation(Window wnd) {
        if (wnd != null) {
            WindowManager.LayoutParams attrs = wnd.getAttributes();
            attrs.screenOrientation = screenOrientation;
            wnd.setAttributes(attrs);
            if (DeviceInfo.EINK_SCREEN) {
                //TODO:
                //EinkScreen.ResetController(mReaderView);
            }

        }
    }

    public int getScreenOrientation() {
        switch (screenOrientation) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                return 0;
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                return 1;
            case ActivityInfo_SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                return 2;
            case ActivityInfo_SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                return 3;
            case ActivityInfo.SCREEN_ORIENTATION_USER:
                return 5;
            default:
                return orientationFromSensor;
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private boolean isReverseLandscape() {
        return screenOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    }

    public boolean isLandscape() {
        if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            return true;
        if (DeviceInfo.getSDKLevel() >= 9 && isReverseLandscape())
            return true;
        return false;
    }

    final static public int ActivityInfo_SCREEN_ORIENTATION_REVERSE_PORTRAIT = 9;
    final static public int ActivityInfo_SCREEN_ORIENTATION_REVERSE_LANDSCAPE = 8;
    final static public int ActivityInfo_SCREEN_ORIENTATION_FULL_SENSOR = 10;

    public void setScreenOrientation(int angle) {
        int newOrientation = screenOrientation;
        boolean level9 = DeviceInfo.getSDKLevel() >= 9;
        switch (angle) {
            case 0:
                newOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; // level9 ? ActivityInfo_SCREEN_ORIENTATION_SENSOR_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case 1:
                newOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; // level9 ? ActivityInfo_SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case 2:
                newOrientation = level9 ? ActivityInfo_SCREEN_ORIENTATION_REVERSE_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case 3:
                newOrientation = level9 ? ActivityInfo_SCREEN_ORIENTATION_REVERSE_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case 4:
                newOrientation = level9 ? ActivityInfo_SCREEN_ORIENTATION_FULL_SENSOR : ActivityInfo.SCREEN_ORIENTATION_SENSOR;
                break;
            case 5:
                newOrientation = ActivityInfo.SCREEN_ORIENTATION_USER;
                break;
        }
        if (newOrientation != screenOrientation) {
            log.d("setScreenOrientation(" + angle + ")");
            screenOrientation = newOrientation;
            setRequestedOrientation(screenOrientation);
            applyScreenOrientation(getWindow());
        }
    }


    private int orientationFromSensor = 0;

    public int getOrientationFromSensor() {
        return orientationFromSensor;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // pass
        orientationFromSensor = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? 1 : 0;
        super.onConfigurationChanged(newConfig);
    }

    private boolean mFullscreen = false;

    public boolean isFullscreen() {
        return mFullscreen;
    }

    public void applyFullscreen(Window wnd) {
        if (mFullscreen) {
            //mContext.getWindow().requestFeature(Window.)
            wnd.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            wnd.setFlags(0,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setSystemUiVisibility();
    }

    public void setFullscreen(boolean fullscreen) {
        if (mFullscreen != fullscreen) {
            mFullscreen = fullscreen;
            applyFullscreen(getWindow());
        }
    }

    public void applyAppSetting(String key, String value) {
        boolean flg = "1".equals(value);
        if (key.equals(PROP_APP_FULLSCREEN)) {
            setFullscreen("1".equals(value));
        } else if (key.equals(PROP_APP_LOCALE)) {
            setLanguage(value);
        } else if (key.equals(PROP_APP_KEY_BACKLIGHT_OFF)) {
            backlightControl.setKeyBacklightDisabled(flg);
        } else if (key.equals(PROP_APP_SCREEN_BACKLIGHT_LOCK)) {
            int n = 0;
            try {
                n = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // ignore
            }
            backlightControl.setScreenBacklightDuration(n);
        } else if (key.equals(PROP_NIGHT_MODE)) {
            setNightMode(flg);
        } else if (key.equals(PROP_APP_SCREEN_UPDATE_MODE)) {
            backlightControl.setScreenUpdateMode(stringToInt(value, 0), getContentView());
        } else if (key.equals(PROP_APP_SCREEN_UPDATE_INTERVAL)) {
            backlightControl.setScreenUpdateInterval(stringToInt(value, 10), getContentView());
        } else if (key.equals(PROP_APP_THEME)) {
            setCurrentTheme(value);
        } else if (key.equals(PROP_APP_SCREEN_ORIENTATION)) {
            int orientation = 0;
            try {
                orientation = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // ignore
            }
            setScreenOrientation(orientation);
        } else if (!DeviceInfo.EINK_SCREEN && PROP_APP_SCREEN_BACKLIGHT.equals(key)) {
            try {
                final int n = Integer.valueOf(value);
                // delay before setting brightness
                BackgroundThread.instance().postGUI(new Runnable() {
                    public void run() {
                        BackgroundThread.instance().postBackground(new Runnable() {
                            public void run() {
                                BackgroundThread.instance().postGUI(new Runnable() {
                                    public void run() {
                                        backlightControl.setScreenBacklightLevel(n);
                                    }
                                });
                            }
                        });
                    }
                }, 100);
            } catch (Exception e) {
                // ignore
            }
        } else if (key.equals(PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS)) {
            Services.getScanner().setHideEmptyDirs(flg);
        }
    }

    private final static int SYSTEM_UI_FLAG_LOW_PROFILE = 1;


    public boolean setSystemUiVisibility() {
        if (DeviceInfo.getSDKLevel() >= DeviceInfo.HONEYCOMB) {
            int flags = 0;
            if (backlightControl.getKeyBacklight() == 0)
                flags |= SYSTEM_UI_FLAG_LOW_PROFILE;
            setSystemUiVisibility(flags);
            return true;
        }
        return false;
    }


    private int lastSystemUiVisibility = -1;

    @TargetApi(11)
    @SuppressLint("NewApi")
    private boolean setSystemUiVisibility(int value) {
        if (DeviceInfo.getSDKLevel() >= DeviceInfo.HONEYCOMB) {
            if (DeviceInfo.getSDKLevel() < 19) {

                boolean a4 = DeviceInfo.getSDKLevel() >= DeviceInfo.ICE_CREAM_SANDWICH;
                if (!a4)
                    value &= SYSTEM_UI_FLAG_LOW_PROFILE;
                if (value == lastSystemUiVisibility && value != SYSTEM_UI_FLAG_LOW_PROFILE)// && a4)
                    return false;
                lastSystemUiVisibility = value;

                View view;
                //if (a4)
                view = getWindow().getDecorView(); // getReaderView();
                //else
                //	view = mContext.getContentView(); // getReaderView();

                if (view == null)
                    return false;
                Method m;
                try {
                    m = view.getClass().getMethod("setSystemUiVisibility", int.class);
                    m.invoke(view, value);
                    return true;
                } catch (SecurityException e) {
                    // ignore
                } catch (NoSuchMethodException e) {
                    // ignore
                } catch (IllegalArgumentException e) {
                    // ignore
                } catch (IllegalAccessException e) {
                    // ignore
                } catch (InvocationTargetException e) {
                    // ignore
                }
            }
        }
        return false;
    }


    public void showToast(int stringResourceId) {
        showToast(stringResourceId, Toast.LENGTH_LONG);
    }

    public void showToast(int stringResourceId, int duration) {
        String s = getString(stringResourceId);
        if (s != null)
            showToast(s, duration);
    }

    public void showToast(String msg) {
        showToast(msg, Toast.LENGTH_LONG);
    }

    public void showToast(String msg, int duration) {
        log.v("showing toast: " + msg);
        if (DeviceInfo.USE_CUSTOM_TOAST) {
            ToastView.showToast(getContentView(), msg, Toast.LENGTH_LONG, ((ReaderApplication) getApplication()).settings().getInt(ReaderView.PROP_FONT_SIZE, 20));
        } else {
            // classic Toast
            Toast toast = Toast.makeText(this, msg, duration);
            toast.show();
        }
    }


    protected View contentView;

    public View getContentView() {
        return contentView;
    }

    public void setContentView(View view) {
        this.contentView = view;
        super.setContentView(view);
        //systemUiVisibilityListenerIsSet = false;
        //updateBackground();
        setCurrentTheme(currentTheme);
    }


    private boolean mNightMode = false;

    public boolean isNightMode() {
        return mNightMode;
    }

    public void setNightMode(boolean nightMode) {
        mNightMode = nightMode;
    }


    public void showHomeScreen() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }


    private static String PREF_HELP_FILE = "HelpFile";

    public String getLastGeneratedHelpFileSignature() {
        SharedPreferences pref = getSharedPreferences(PREF_FILE, 0);
        String res = pref.getString(PREF_HELP_FILE, null);
        return res;
    }

    public void setLastGeneratedHelpFileSignature(String v) {
        SharedPreferences pref = getSharedPreferences(PREF_FILE, 0);
        pref.edit().putString(PREF_HELP_FILE, v).commit();
    }


    private String currentLanguage;

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void setLanguage(String lang) {
        setLanguage(Lang.byCode(lang));
    }

    public void setLanguage(Lang lang) {
        try {
            Resources res = getResources();
            // Change locale settings in the app.
            DisplayMetrics dm = res.getDisplayMetrics();
            android.content.res.Configuration conf = res.getConfiguration();
            conf.locale = (lang == Lang.DEFAULT) ? defaultLocale : lang.getLocale();
            currentLanguage = (lang == Lang.DEFAULT) ? Lang.getCode(defaultLocale) : lang.code;
            res.updateConfiguration(conf, dm);
        } catch (Exception e) {
            log.e("error while setting locale " + lang, e);
        }
    }

    // Store system locale here, on class creation
    private static final Locale defaultLocale = Locale.getDefault();


    static public int stringToInt(String value, int defValue) {
        if (value == null)
            return defValue;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }


    public void showNotice(int questionResourceId, final Runnable action, final Runnable cancelAction) {
        NoticeDialog dlg = new NoticeDialog(this, action, cancelAction);
        dlg.show();
    }

    public void askConfirmation(int questionResourceId, final Runnable action) {
        askConfirmation(questionResourceId, action, null);
    }

    public void askConfirmation(int questionResourceId, final Runnable action, final Runnable cancelAction) {
        AlertDialog.Builder dlg = new AlertDialog.Builder(this);

        final TextView myView = new TextView(getApplicationContext());
        myView.setText(questionResourceId);
        //myView.setTextSize(12);
        dlg.setView(myView);
        //dlg.setTitle(questionResourceId);
        dlg.setPositiveButton(R.string.dlg_button_ok, new OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                action.run();
            }
        });
        dlg.setNegativeButton(R.string.dlg_button_cancel, new OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                if (cancelAction != null)
                    cancelAction.run();
            }
        });
        dlg.show();
    }

    public void askConfirmation(String question, final Runnable action) {
        AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        dlg.setTitle(question);
        dlg.setPositiveButton(R.string.dlg_button_ok, new OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                action.run();
            }
        });
        dlg.setNegativeButton(R.string.dlg_button_cancel, new OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                // do nothing
            }
        });
        dlg.show();
    }

    public void directoryUpdated(FileInfo dir) {
        // override it to use
    }

    public void onSettingsChanged(Properties props, Properties oldProps) {
        // override for specific actions

    }

    public void showActionsPopupMenu(final ReaderAction[] actions, final CRToolBar.OnActionHandler onActionHandler) {
        ArrayList<ReaderAction> list = new ArrayList<ReaderAction>(actions.length);
        for (ReaderAction a : actions)
            list.add(a);
        showActionsPopupMenu(list, onActionHandler);
    }

    public void showActionsPopupMenu(final ArrayList<ReaderAction> actions, final CRToolBar.OnActionHandler onActionHandler) {
        registerForContextMenu(contentView);
        contentView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                                            ContextMenuInfo menuInfo) {
                //populate only it is not populated by children
                if (menu.size() == 0) {
                    int order = 0;
                    for (final ReaderAction action : actions) {
                        MenuItem item = menu.add(0, action.menuItemId, order++, action.nameId);
                        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                return onActionHandler.onActionSelected(action);
                            }
                        });
                    }
                }
            }
        });
        contentView.showContextMenu();
    }

    public void showBrowserOptionsDialog() {
        OptionsDialog dlg = new OptionsDialog(BaseActivity.this, null, null, OptionsDialog.Mode.BROWSER);
        dlg.show();
    }

    private int currentProfile = 0;

    public int getCurrentProfile() {
        if (currentProfile == 0) {
            currentProfile = mSettingsManager.getInt(PROP_PROFILE_NUMBER, 1);
            if (currentProfile < 1 || currentProfile > MAX_PROFILES)
                currentProfile = 1;
        }
        return currentProfile;
    }

    public void setCurrentProfile(int profile) {
        if (profile == 0 || profile == getCurrentProfile())
            return;
        log.i("Switching from profile " + currentProfile + " to " + profile);
        mSettingsManager.saveSettings(currentProfile, null);
        final Properties loadedSettings = mSettingsManager.loadSettings(profile);
        mSettingsManager.setSettings(loadedSettings, 0, true);
        currentProfile = profile;
    }

    public void setSetting(String name, String value, boolean notify) {
        mSettingsManager.setSetting(name, value, notify);
    }

    public void setSettings(Properties settings, int delayMillis, boolean notify) {
        mSettingsManager.setSettings(settings, delayMillis, notify);
    }

    public void notifySettingsChanged() {
        setSettings(mSettingsManager.get(), -1, true);
    }


    private Boolean hasHardwareMenuKey = null;

    public boolean hasHardwareMenuKey() {
        if (hasHardwareMenuKey == null) {
            ViewConfiguration vc = ViewConfiguration.get(this);
            if (DeviceInfo.getSDKLevel() >= 14) {
                //boolean vc.hasPermanentMenuKey();
                try {
                    Method m = vc.getClass().getMethod("hasPermanentMenuKey", new Class<?>[]{});
                    try {
                        hasHardwareMenuKey = (Boolean) m.invoke(vc, new Object[]{});
                    } catch (IllegalArgumentException e) {
                        hasHardwareMenuKey = false;
                    } catch (IllegalAccessException e) {
                        hasHardwareMenuKey = false;
                    } catch (InvocationTargetException e) {
                        hasHardwareMenuKey = false;
                    }
                } catch (NoSuchMethodException e) {
                    hasHardwareMenuKey = false;
                }
            }
            if (hasHardwareMenuKey == null) {
                if (DeviceInfo.EINK_SCREEN)
                    hasHardwareMenuKey = false;
                else if (DeviceInfo.getSDKLevel() < DeviceInfo.ICE_CREAM_SANDWICH)
                    hasHardwareMenuKey = true;
                else
                    hasHardwareMenuKey = false;
            }
        }
        return hasHardwareMenuKey;
    }
    // Dictionary support

}
