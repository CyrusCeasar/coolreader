package org.coolreader.crengine;

import android.content.Context;
import android.util.Log;


import org.coolreader.crengine.reader.ReaderView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import cn.cyrus.translater.base.LogUtil;


public class SettingsManager implements Settings {

    public static final Logger log = L.create("cr");

    public Context mContext;
    public Properties mSettings;

    private final File defaultSettingsDir;
    private static SettingsManager instance;

    public static SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context);
        }
        return instance;
    }

    private SettingsManager(Context activity) {
        this.mContext = activity;
        defaultSettingsDir = activity.getDir("settings", Context.MODE_PRIVATE);
        mSettings = loadSettings();
    }

    //int lastSaveId = 0;
    public void setSettings(Properties settings, int delayMillis, boolean notify) {
        Properties oldSettings = mSettings;
        mSettings = new Properties(settings);
        saveSettings(mSettings);
//			if (notify)
//				mContext.onSettingsChanged(mSettings, oldSettings);
    }

    public void setSetting(String name, String value, boolean notify) {
        Properties props = new Properties(mSettings);
        if (value.equals(mSettings.getProperty(name)))
            return;
        props.setProperty(name, value);
        setSettings(props, 1000, notify);
    }

    private static class DefKeyAction {
        public int keyCode;
        public int type;
        public ReaderAction action;

        public DefKeyAction(int keyCode, int type, ReaderAction action) {
            this.keyCode = keyCode;
            this.type = type;
            this.action = action;
        }

        public String getProp() {
            return ReaderView.PROP_APP_KEY_ACTIONS_PRESS + ReaderAction.getTypeString(type) + keyCode;
        }
    }

    private static class DefTapAction {
        public int zone;
        public boolean longPress;
        public ReaderAction action;

        public DefTapAction(int zone, boolean longPress, ReaderAction action) {
            this.zone = zone;
            this.longPress = longPress;
            this.action = action;
        }
    }

    private static DefTapAction[] DEF_TAP_ACTIONS = {
            new DefTapAction(1, false, ReaderAction.PAGE_UP),
            new DefTapAction(2, false, ReaderAction.PAGE_UP),
            new DefTapAction(4, false, ReaderAction.PAGE_UP),
            new DefTapAction(1, true, ReaderAction.GO_BACK), // back by link
            new DefTapAction(2, true, ReaderAction.TOGGLE_DAY_NIGHT),
            new DefTapAction(4, true, ReaderAction.PAGE_UP_10),
            new DefTapAction(3, false, ReaderAction.PAGE_DOWN),
            new DefTapAction(6, false, ReaderAction.PAGE_DOWN),
            new DefTapAction(7, false, ReaderAction.PAGE_DOWN),
            new DefTapAction(8, false, ReaderAction.PAGE_DOWN),
            new DefTapAction(9, false, ReaderAction.PAGE_DOWN),
            new DefTapAction(3, true, ReaderAction.TOGGLE_AUTOSCROLL),
            new DefTapAction(6, true, ReaderAction.PAGE_DOWN_10),
            new DefTapAction(7, true, ReaderAction.PAGE_DOWN_10),
            new DefTapAction(8, true, ReaderAction.PAGE_DOWN_10),
            new DefTapAction(9, true, ReaderAction.PAGE_DOWN_10),
            new DefTapAction(5, false, ReaderAction.READER_MENU),
            new DefTapAction(5, true, ReaderAction.OPTIONS),
    };
    private static DefTapAction[] DEF_TAP_ACTIONS2 = {
            new DefTapAction(1, false, ReaderAction.START_SELECTION),
            new DefTapAction(2, false, ReaderAction.START_SELECTION),
            new DefTapAction(4, false, ReaderAction.START_SELECTION),
            new DefTapAction(1, true, ReaderAction.START_SELECTION), // back by link
            new DefTapAction(2, true, ReaderAction.START_SELECTION),
            new DefTapAction(4, true, ReaderAction.START_SELECTION),
            new DefTapAction(3, false, ReaderAction.START_SELECTION),
            new DefTapAction(6, false, ReaderAction.START_SELECTION),
            new DefTapAction(7, false, ReaderAction.START_SELECTION),
            new DefTapAction(8, false, ReaderAction.START_SELECTION),
            new DefTapAction(9, false, ReaderAction.START_SELECTION),
            new DefTapAction(3, true, ReaderAction.START_SELECTION),
            new DefTapAction(6, true, ReaderAction.START_SELECTION),
            new DefTapAction(7, true, ReaderAction.START_SELECTION),
            new DefTapAction(8, true, ReaderAction.START_SELECTION),
            new DefTapAction(9, true, ReaderAction.START_SELECTION),
            new DefTapAction(5, false, ReaderAction.START_SELECTION),
            new DefTapAction(5, true, ReaderAction.START_SELECTION),
    };

    private boolean isValidFontFace(String face) {
        String[] fontFaces = Engine.getFontFaceList();
        if (fontFaces == null)
            return true;
        for (String item : fontFaces) {
            if (item.equals(face))
                return true;
        }
        return false;
    }

    private boolean applyDefaultFont(Properties props, String propName, String defFontFace) {
        String currentValue = props.getProperty(propName);
        boolean changed = false;
        if (currentValue == null) {
            currentValue = defFontFace;
            changed = true;
        }
        if (!isValidFontFace(currentValue)) {
            if (isValidFontFace("Droid Sans"))
                currentValue = "Droid Sans";
            else if (isValidFontFace("Roboto"))
                currentValue = "Roboto";
            else if (isValidFontFace("Droid Serif"))
                currentValue = "Droid Serif";
            else if (isValidFontFace("Arial"))
                currentValue = "Arial";
            else if (isValidFontFace("Times New Roman"))
                currentValue = "Times New Roman";
            else if (isValidFontFace("Droid Sans Fallback"))
                currentValue = "Droid Sans Fallback";
            else {
                String[] fontFaces = Engine.getFontFaceList();
                if (fontFaces != null)
                    currentValue = fontFaces[0];
            }
            changed = true;
        }
        if (changed)
            props.setProperty(propName, currentValue);
        return changed;
    }

    public boolean fixFontSettings(Properties props) {
        boolean res = false;
        res = applyDefaultFont(props, ReaderView.PROP_FONT_FACE, DeviceInfo.DEF_FONT_FACE) || res;
        res = applyDefaultFont(props, ReaderView.PROP_STATUS_FONT_FACE, DeviceInfo.DEF_FONT_FACE) || res;
        res = applyDefaultFont(props, ReaderView.PROP_FALLBACK_FONT_FACE, "Droid Sans Fallback") || res;
        return res;
    }

    public Properties loadSettings(Context activity, File file) {
        Properties props = new Properties();

        if (file.exists() && !DEBUG_RESET_OPTIONS) {
            try {
                FileInputStream is = new FileInputStream(file);
                props.load(is);
                log.v("" + props.size() + " settings items loaded from file " + propsFile.getAbsolutePath());
            } catch (Exception e) {
                log.e("error while reading settings");
            }
        }


        boolean menuTapActionFound = false;
        for (DefTapAction ka : DEF_TAP_ACTIONS) {
            String paramName = ka.longPress ? ReaderView.PROP_APP_TAP_ZONE_ACTIONS_TAP + ".long." + ka.zone : ReaderView.PROP_APP_TAP_ZONE_ACTIONS_TAP + "." + ka.zone;
            String value = props.getProperty(paramName);
            Log.d(paramName + "   " + value, LogUtil.Companion.getTAG());
            if (ReaderAction.READER_MENU.id.equals(value))
                menuTapActionFound = true;
        }

        // default tap zone actions
        for (DefTapAction ka : DEF_TAP_ACTIONS2) {
            String paramName = ka.longPress ? ReaderView.PROP_APP_TAP_ZONE_ACTIONS_TAP + ".long." + ka.zone : ReaderView.PROP_APP_TAP_ZONE_ACTIONS_TAP + "." + ka.zone;
            Log.d(paramName + "   " + ka.action, LogUtil.Companion.getTAG());
	       /* 	if (ka.zone == 5 && !activity.hasHardwareMenuKey() && !menuTapActionFound && !menuKeyActionFound) {
	        		// force assignment of central tap zone
	        		props.setProperty(paramName, ka.action.id);
	        	} else {*/
            props.applyDefault(paramName, ka.action.id);
//	        	}
        }

        if (DeviceInfo.EINK_NOOK) {
            props.applyDefault(ReaderView.PROP_PAGE_ANIMATION, ReaderView.PAGE_ANIMATION_NONE);
        } else {
            props.applyDefault(ReaderView.PROP_PAGE_ANIMATION, ReaderView.PAGE_ANIMATION_SLIDE2);
        }

        props.applyDefault(ReaderView.PROP_APP_LOCALE, Settings.Lang.DEFAULT.code);

        props.applyDefault(ReaderView.PROP_APP_THEME, DeviceInfo.FORCE_LIGHT_THEME ? "WHITE" : "LIGHT");
        props.applyDefault(ReaderView.PROP_APP_THEME_DAY, DeviceInfo.FORCE_LIGHT_THEME ? "WHITE" : "LIGHT");
        props.applyDefault(ReaderView.PROP_APP_THEME_NIGHT, DeviceInfo.FORCE_LIGHT_THEME ? "BLACK" : "DARK");
        props.applyDefault(ReaderView.PROP_APP_SELECTION_PERSIST, "0");
        props.applyDefault(ReaderView.PROP_APP_SCREEN_BACKLIGHT_LOCK, "3");
        if ("1".equals(props.getProperty(ReaderView.PROP_APP_SCREEN_BACKLIGHT_LOCK)))
            props.setProperty(ReaderView.PROP_APP_SCREEN_BACKLIGHT_LOCK, "3");
        props.applyDefault(ReaderView.PROP_APP_MOTION_TIMEOUT, "0");
        props.applyDefault(ReaderView.PROP_APP_BOOK_PROPERTY_SCAN_ENABLED, "1");
        props.applyDefault(ReaderView.PROP_APP_KEY_BACKLIGHT_OFF, DeviceInfo.SAMSUNG_BUTTONS_HIGHLIGHT_PATCH ? "0" : "1");
        props.applyDefault(ReaderView.PROP_LANDSCAPE_PAGES, DeviceInfo.ONE_COLUMN_IN_LANDSCAPE ? "0" : "1");
        // autodetect best initial font size based on display resolution
     /*   int screenHeight = displayMetrics.heightPixels;
        int screenWidth = displayMetrics.widthPixels;
        if (screenWidth > screenHeight)
            screenWidth = screenHeight;*/
        int fontSize = 20;
        int statusFontSize = 16;
        String hmargin = "4";
        String vmargin = "2";
  /*      if (screenWidth <= 320) {
            fontSize = 20;
            statusFontSize = 16;
            hmargin = "4";
            vmargin = "2";
        } else if (screenWidth <= 400) {
            fontSize = 24;
            statusFontSize = 20;
            hmargin = "10";
            vmargin = "4";
        } else if (screenWidth <= 600) {
            fontSize = 28;
            statusFontSize = 24;
            hmargin = "20";
            vmargin = "8";
        } else if*//* (screenWidth <= 800) {*/
        fontSize = 32;
        statusFontSize = 28;
        hmargin = "25";
        vmargin = "15";
       /* } else {
            fontSize = 48;
            statusFontSize = 32;
            hmargin = "30";
            vmargin = "20";
        }*/
        if (DeviceInfo.DEF_FONT_SIZE != null)
            fontSize = DeviceInfo.DEF_FONT_SIZE;

        int statusLocation = props.getInt(PROP_STATUS_LOCATION, VIEWER_STATUS_PAGE);
        if (statusLocation == VIEWER_STATUS_BOTTOM || statusLocation == VIEWER_STATUS_TOP)
            statusLocation = VIEWER_STATUS_PAGE;
        props.setInt(PROP_STATUS_LOCATION, statusLocation);


        fixFontSettings(props);
        props.applyDefault(ReaderView.PROP_FONT_SIZE, String.valueOf(fontSize));
        props.applyDefault(ReaderView.PROP_FONT_HINTING, "2");
        props.applyDefault(ReaderView.PROP_STATUS_FONT_SIZE, DeviceInfo.EINK_NOOK ? "15" : String.valueOf(statusFontSize));
        props.applyDefault(ReaderView.PROP_FONT_COLOR, "#000000");
        props.applyDefault(ReaderView.PROP_FONT_COLOR_DAY, "#000000");
        props.applyDefault(ReaderView.PROP_FONT_COLOR_NIGHT, "#D0B070");
        props.applyDefault(ReaderView.PROP_BACKGROUND_COLOR, "#FFFFFF");
        props.applyDefault(ReaderView.PROP_BACKGROUND_COLOR_DAY, "#FFFFFF");
        props.applyDefault(ReaderView.PROP_BACKGROUND_COLOR_NIGHT, "#101010");
        props.applyDefault(ReaderView.PROP_STATUS_FONT_COLOR, "#FF000000"); // don't use separate color
        props.applyDefault(ReaderView.PROP_STATUS_FONT_COLOR_DAY, "#FF000000"); // don't use separate color
        props.applyDefault(ReaderView.PROP_STATUS_FONT_COLOR_NIGHT, "#80000000"); // don't use separate color
        props.setProperty(ReaderView.PROP_ROTATE_ANGLE, "0"); // crengine's rotation will not be user anymore
        props.setProperty(ReaderView.PROP_DISPLAY_INVERSE, "0");
        props.applyDefault(ReaderView.PROP_APP_FULLSCREEN, "0");
        props.applyDefault(ReaderView.PROP_APP_VIEW_AUTOSCROLL_SPEED, "1500");
        props.applyDefault(ReaderView.PROP_APP_SCREEN_BACKLIGHT, "-1");
        props.applyDefault(ReaderView.PROP_SHOW_BATTERY, "1");
        props.applyDefault(ReaderView.PROP_SHOW_POS_PERCENT, "0");
        props.applyDefault(ReaderView.PROP_SHOW_PAGE_COUNT, "1");
        props.applyDefault(ReaderView.PROP_SHOW_TIME, "1");
        props.applyDefault(ReaderView.PROP_FONT_ANTIALIASING, "2");
        props.applyDefault(ReaderView.PROP_APP_GESTURE_PAGE_FLIPPING, "1");
        props.applyDefault(ReaderView.PROP_APP_SHOW_COVERPAGES, "1");
        props.applyDefault(ReaderView.PROP_APP_COVERPAGE_SIZE, "1");
        props.applyDefault(ReaderView.PROP_APP_SCREEN_ORIENTATION, DeviceInfo.EINK_SCREEN ? "0" : "4"); // "0"
        props.applyDefault(ReaderView.PROP_CONTROLS_ENABLE_VOLUME_KEYS, "1");
        props.applyDefault(ReaderView.PROP_APP_TAP_ZONE_HILIGHT, "0");
        props.applyDefault(ReaderView.PROP_APP_BOOK_SORT_ORDER, FileInfo.DEF_SORT_ORDER.name());
        props.applyDefault(ReaderView.PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS, "0");
        props.applyDefault(ReaderView.PROP_APP_SELECTION_ACTION, "1");
        props.applyDefault(ReaderView.PROP_APP_MULTI_SELECTION_ACTION, "0");

        props.applyDefault(ReaderView.PROP_IMG_SCALING_ZOOMOUT_BLOCK_MODE, "1");
        props.applyDefault(ReaderView.PROP_IMG_SCALING_ZOOMIN_BLOCK_MODE, "1");
        props.applyDefault(ReaderView.PROP_IMG_SCALING_ZOOMOUT_INLINE_MODE, "1");
        props.applyDefault(ReaderView.PROP_IMG_SCALING_ZOOMIN_INLINE_MODE, "1");
        props.applyDefault(ReaderView.PROP_IMG_SCALING_ZOOMOUT_BLOCK_SCALE, "0");
        props.applyDefault(ReaderView.PROP_IMG_SCALING_ZOOMIN_BLOCK_SCALE, "0");
        props.applyDefault(ReaderView.PROP_IMG_SCALING_ZOOMOUT_INLINE_SCALE, "0");
        props.applyDefault(ReaderView.PROP_IMG_SCALING_ZOOMIN_INLINE_SCALE, "0");

        props.applyDefault(ReaderView.PROP_PAGE_MARGIN_LEFT, hmargin);
        props.applyDefault(ReaderView.PROP_PAGE_MARGIN_RIGHT, hmargin);
        props.applyDefault(ReaderView.PROP_PAGE_MARGIN_TOP, vmargin);
        props.applyDefault(ReaderView.PROP_PAGE_MARGIN_BOTTOM, vmargin);

        props.applyDefault(ReaderView.PROP_APP_SCREEN_UPDATE_MODE, "0");
        props.applyDefault(ReaderView.PROP_APP_SCREEN_UPDATE_INTERVAL, "10");

        props.applyDefault(ReaderView.PROP_NIGHT_MODE, "0");
        if (DeviceInfo.FORCE_LIGHT_THEME) {
            props.applyDefault(ReaderView.PROP_PAGE_BACKGROUND_IMAGE, Engine.NO_TEXTURE.id);
        } else {
            if (props.getBool(ReaderView.PROP_NIGHT_MODE, false))
                props.applyDefault(ReaderView.PROP_PAGE_BACKGROUND_IMAGE, Engine.DEF_NIGHT_BACKGROUND_TEXTURE);
            else
                props.applyDefault(ReaderView.PROP_PAGE_BACKGROUND_IMAGE, Engine.DEF_DAY_BACKGROUND_TEXTURE);
        }
        props.applyDefault(ReaderView.PROP_PAGE_BACKGROUND_IMAGE_DAY, Engine.DEF_DAY_BACKGROUND_TEXTURE);
        props.applyDefault(ReaderView.PROP_PAGE_BACKGROUND_IMAGE_NIGHT, Engine.DEF_NIGHT_BACKGROUND_TEXTURE);

        props.applyDefault(ReaderView.PROP_FONT_GAMMA, DeviceInfo.EINK_SCREEN ? "1.5" : "1.0");

        props.setProperty(ReaderView.PROP_MIN_FILE_SIZE_TO_CACHE, "100000");
        props.setProperty(ReaderView.PROP_FORCED_MIN_FILE_SIZE_TO_CACHE, "32768");
        props.applyDefault(ReaderView.PROP_HYPHENATION_DICT, Engine.HyphDict.RUSSIAN.toString());
        props.applyDefault(ReaderView.PROP_APP_FILE_BROWSER_SIMPLE_MODE, "0");

        props.applyDefault(ReaderView.PROP_STATUS_LOCATION, Settings.VIEWER_STATUS_PAGE);
        //props.applyDefault(ReaderView.PROP_TOOLBAR_LOCATION, DeviceInfo.getSDKLevel() < DeviceInfo.HONEYCOMB ? Settings.VIEWER_TOOLBAR_NONE : Settings.VIEWER_TOOLBAR_SHORT_SIDE);
        props.applyDefault(ReaderView.PROP_TOOLBAR_LOCATION, Settings.VIEWER_TOOLBAR_NONE);
        props.applyDefault(ReaderView.PROP_TOOLBAR_HIDE_IN_FULLSCREEN, "0");


        if (!DeviceInfo.EINK_SCREEN) {
            props.applyDefault(ReaderView.PROP_APP_HIGHLIGHT_BOOKMARKS, "1");
            props.applyDefault(ReaderView.PROP_HIGHLIGHT_SELECTION_COLOR, "#AAAAAA");
            props.applyDefault(ReaderView.PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT, "#AAAA55");
            props.applyDefault(ReaderView.PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION, "#C07070");
            props.applyDefault(ReaderView.PROP_HIGHLIGHT_SELECTION_COLOR_DAY, "#AAAAAA");
            props.applyDefault(ReaderView.PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_DAY, "#AAAA55");
            props.applyDefault(ReaderView.PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_DAY, "#C07070");
            props.applyDefault(ReaderView.PROP_HIGHLIGHT_SELECTION_COLOR_NIGHT, "#808080");
            props.applyDefault(ReaderView.PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_NIGHT, "#A09060");
            props.applyDefault(ReaderView.PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_NIGHT, "#906060");
        } else {
            props.applyDefault(ReaderView.PROP_APP_HIGHLIGHT_BOOKMARKS, "2");
            props.applyDefault(ReaderView.PROP_HIGHLIGHT_SELECTION_COLOR, "#808080");
            props.applyDefault(ReaderView.PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT, "#000000");
            props.applyDefault(ReaderView.PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION, "#000000");
        }

        return props;
    }

    public File getSettingsFile(int profile) {
        if (profile == 0)
            return propsFile;
        return new File(propsFile.getAbsolutePath() + ".profile" + profile);
    }

    File propsFile;
    private static final String SETTINGS_FILE_NAME = "cr3.ini";
    private static boolean DEBUG_RESET_OPTIONS = false;

    private Properties loadSettings() {
        File[] dataDirs = Engine.getDataDirectories(null, false, true);
        File existingFile = null;
        for (File dir : dataDirs) {
            File f = new File(dir, SETTINGS_FILE_NAME);
            if (f.exists() && f.isFile()) {
                existingFile = f;
                break;
            }
        }
        if (existingFile != null) {
            propsFile = existingFile;
        } else {
            File propsDir = defaultSettingsDir;
            propsFile = new File(propsDir, SETTINGS_FILE_NAME);
            File dataDir = Engine.getExternalSettingsDir();
            if (dataDir != null) {
                log.d("external settings dir: " + dataDir);
                propsFile = Engine.checkOrMoveFile(dataDir, propsDir, SETTINGS_FILE_NAME);
            } else {
                propsDir.mkdirs();
            }
        }

        Properties props = loadSettings(mContext, propsFile);

        return props;
    }

    public Properties loadSettings(int profile) {
        File f = getSettingsFile(profile);
        if (!f.exists() && profile != 0)
            f = getSettingsFile(0);
        Properties res = loadSettings(mContext, f);
        if (profile != 0) {
            res = filterProfileSettings(res);
            res.setInt(Settings.PROP_PROFILE_NUMBER, profile);
        }
        return res;
    }

    public static Properties filterProfileSettings(Properties settings) {
        Properties res = new Properties();
        res.entrySet();
        for (Object k : settings.keySet()) {
            String key = (String) k;
            String value = settings.getProperty(key);
            boolean found = false;
            for (String pattern : Settings.PROFILE_SETTINGS) {
                if (pattern.endsWith("*")) {
                    if (key.startsWith(pattern.substring(0, pattern.length() - 1))) {
                        found = true;
                        break;
                    }
                } else if (pattern.equalsIgnoreCase(key)) {
                    found = true;
                    break;
                } else if (key.startsWith("styles.")) {
                    found = true;
                    break;
                }
            }
            if (found) {
                res.setProperty(key, value);
            }
        }
        return res;
    }

    public void saveSettings(int profile, Properties settings) {
        if (settings == null)
            settings = mSettings;
        File f = getSettingsFile(profile);
        if (profile != 0) {
            settings = filterProfileSettings(settings);
            settings.setInt(Settings.PROP_PROFILE_NUMBER, profile);
        }
        saveSettings(f, settings);
    }


    public void saveSettings(File f, Properties settings) {
        try {
            log.v("saveSettings()");
            FileOutputStream os = new FileOutputStream(f);
            settings.store(os, "Cool Reader 3 settings");
            log.i("Settings successfully saved to file " + f.getAbsolutePath());
        } catch (Exception e) {
            log.e("exception while saving settings", e);
        }
    }

    public void saveSettings(Properties settings) {
        saveSettings(propsFile, settings);
    }


    public boolean getBool(String name, boolean defaultValue) {
        return mSettings.getBool(name, defaultValue);
    }

    public int getInt(String name, int defaultValue) {
        return mSettings.getInt(name, defaultValue);
    }

    public Properties get() {
        return new Properties(mSettings);
    }

}