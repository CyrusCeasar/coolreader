package org.coolreader.crengine;

import android.util.Log;

import org.coolreader.R;

import java.util.Locale;

public interface Settings {
    String PROP_PAGE_BACKGROUND_IMAGE = "background.image";
    String PROP_PAGE_BACKGROUND_IMAGE_DAY = "background.image.day";
    String PROP_PAGE_BACKGROUND_IMAGE_NIGHT = "background.image.night";
    String PROP_NIGHT_MODE = "crengine.night.mode";
    String PROP_FONT_COLOR_DAY = "font.color.day";
    String PROP_BACKGROUND_COLOR_DAY = "background.color.day";
    String PROP_FONT_COLOR_NIGHT = "font.color.night";
    String PROP_BACKGROUND_COLOR_NIGHT = "background.color.night";
    String PROP_FONT_COLOR = "font.color.default";
    String PROP_BACKGROUND_COLOR = "background.color.default";
    String PROP_FONT_ANTIALIASING = "font.antialiasing.mode";
    String PROP_FONT_FACE = "font.face.default";
    String PROP_FONT_HINTING = "font.hinting.mode";
    String PROP_FONT_GAMMA = "font.gamma";
    String PROP_FONT_GAMMA_DAY = "font.gamma.day";
    String PROP_FONT_GAMMA_NIGHT = "font.gamma.night";
    String PROP_FONT_WEIGHT_EMBOLDEN = "font.face.weight.embolden";
    String PROP_TXT_OPTION_PREFORMATTED = "crengine.file.txt.preformatted";
    String PROP_LOG_FILENAME = "crengine.log.filename";
    String PROP_LOG_LEVEL = "crengine.log.level";
    String PROP_LOG_AUTOFLUSH = "crengine.log.autoflush";
    String PROP_FONT_SIZE = "crengine.font.size";
    String PROP_FALLBACK_FONT_FACE = "crengine.font.fallback.face";
    String PROP_STATUS_FONT_COLOR = "crengine.page.header.font.color";
    String PROP_STATUS_FONT_COLOR_DAY = "crengine.page.header.font.color.day";
    String PROP_STATUS_FONT_COLOR_NIGHT = "crengine.page.header.font.color.night";
    String PROP_STATUS_FONT_FACE = "crengine.page.header.font.face";
    String PROP_STATUS_FONT_SIZE = "crengine.page.header.font.size";
    String PROP_STATUS_CHAPTER_MARKS = "crengine.page.header.chapter.marks";
    String PROP_PAGE_MARGIN_TOP = "crengine.page.margin.top";
    String PROP_PAGE_MARGIN_BOTTOM = "crengine.page.margin.bottom";
    String PROP_PAGE_MARGIN_LEFT = "crengine.page.margin.left";
    String PROP_PAGE_MARGIN_RIGHT = "crengine.page.margin.right";
    String PROP_PAGE_VIEW_MODE = "crengine.page.view.mode"; // pages/scroll
    String PROP_PAGE_ANIMATION = "crengine.page.animation";
    String PROP_INTERLINE_SPACE = "crengine.interline.space";
    String PROP_ROTATE_ANGLE = "window.rotate.angle";
    String PROP_EMBEDDED_STYLES = "crengine.doc.embedded.styles.enabled";
    String PROP_EMBEDDED_FONTS = "crengine.doc.embedded.fonts.enabled";
    String PROP_DISPLAY_INVERSE = "crengine.display.inverse";
//    public static final String PROP_DISPLAY_FULL_UPDATE_INTERVAL ="crengine.display.full.update.interval";
//    public static final String PROP_DISPLAY_TURBO_UPDATE_MODE ="crengine.display.turbo.update";

    String PROP_STATUS_LOCATION = "viewer.status.location";
    String PROP_TOOLBAR_LOCATION = "viewer.toolbar.location2";
    String PROP_TOOLBAR_HIDE_IN_FULLSCREEN = "viewer.toolbar.fullscreen.hide";

    String PROP_STATUS_LINE = "window.status.line";
    String PROP_BOOKMARK_ICONS = "crengine.bookmarks.icons";
    String PROP_FOOTNOTES = "crengine.footnotes";
    String PROP_SHOW_TIME = "window.status.clock";
    String PROP_SHOW_TITLE = "window.status.title";
    String PROP_SHOW_BATTERY = "window.status.battery";
    String PROP_SHOW_BATTERY_PERCENT = "window.status.battery.percent";
    String PROP_SHOW_POS_PERCENT = "window.status.pos.percent";
    String PROP_SHOW_PAGE_COUNT = "window.status.pos.page.count";
    String PROP_SHOW_PAGE_NUMBER = "window.status.pos.page.number";
    String PROP_FONT_KERNING_ENABLED = "font.kerning.enabled";
    String PROP_FLOATING_PUNCTUATION = "crengine.style.floating.punctuation.enabled";
    String PROP_LANDSCAPE_PAGES = "window.landscape.pages";
    String PROP_HYPHENATION_DICT = "crengine.hyphenation.dictionary.code"; // non-crengine
    String PROP_AUTOSAVE_BOOKMARKS = "crengine.autosave.bookmarks";

    String PROP_PROFILE_NUMBER = "crengine.profile.number"; // current settings profile number
    String PROP_APP_SETTINGS_SHOW_ICONS = "app.settings.show.icons";
    String PROP_APP_KEY_BACKLIGHT_OFF = "app.key.backlight.disabled";

    // image scaling settings
    // mode: 0=disabled, 1=integer scaling factors, 2=free scaling
    // scale: 0=auto based on font size, 1=no zoom, 2=scale up to *2, 3=scale up to *3
    String PROP_IMG_SCALING_ZOOMIN_INLINE_MODE = "crengine.image.scaling.zoomin.inline.mode";
    String PROP_IMG_SCALING_ZOOMIN_INLINE_SCALE = "crengine.image.scaling.zoomin.inline.scale";
    String PROP_IMG_SCALING_ZOOMOUT_INLINE_MODE = "crengine.image.scaling.zoomout.inline.mode";
    String PROP_IMG_SCALING_ZOOMOUT_INLINE_SCALE = "crengine.image.scaling.zoomout.inline.scale";
    String PROP_IMG_SCALING_ZOOMIN_BLOCK_MODE = "crengine.image.scaling.zoomin.block.mode";
    String PROP_IMG_SCALING_ZOOMIN_BLOCK_SCALE = "crengine.image.scaling.zoomin.block.scale";
    String PROP_IMG_SCALING_ZOOMOUT_BLOCK_MODE = "crengine.image.scaling.zoomout.block.mode";
    String PROP_IMG_SCALING_ZOOMOUT_BLOCK_SCALE = "crengine.image.scaling.zoomout.block.scale";

    String PROP_FORMAT_MIN_SPACE_CONDENSING_PERCENT = "crengine.style.space.condensing.percent";

    String PROP_MIN_FILE_SIZE_TO_CACHE = "crengine.cache.filesize.min";
    String PROP_FORCED_MIN_FILE_SIZE_TO_CACHE = "crengine.cache.forced.filesize.min";
    String PROP_PROGRESS_SHOW_FIRST_PAGE = "crengine.progress.show.first.page";

    String PROP_CONTROLS_ENABLE_VOLUME_KEYS = "app.controls.volume.keys.enabled";

    String PROP_APP_FULLSCREEN = "app.fullscreen";
    String PROP_APP_BOOK_PROPERTY_SCAN_ENABLED = "app.browser.fileprops.scan.enabled";
    String PROP_APP_SHOW_COVERPAGES = "app.browser.coverpages";
    String PROP_APP_COVERPAGE_SIZE = "app.browser.coverpage.size"; // 0==small, 2==BIG
    String PROP_APP_SCREEN_ORIENTATION = "app.screen.orientation";
    String PROP_APP_SCREEN_BACKLIGHT = "app.screen.backlight";
    String PROP_APP_MOTION_TIMEOUT = "app.motion.timeout";
    String PROP_APP_SCREEN_BACKLIGHT_DAY = "app.screen.backlight.day";
    String PROP_APP_SCREEN_BACKLIGHT_NIGHT = "app.screen.backlight.night";
    String PROP_APP_DOUBLE_TAP_SELECTION = "app.controls.doubletap.selection";
    String PROP_APP_TAP_ZONE_ACTIONS_TAP = "app.tapzone.action.tap";
    String PROP_APP_KEY_ACTIONS_PRESS = "app.key.action.press";
    String PROP_APP_TRACKBALL_DISABLED = "app.trackball.disabled";
    String PROP_APP_SCREEN_BACKLIGHT_LOCK = "app.screen.backlight.lock.enabled";
    String PROP_APP_TAP_ZONE_HILIGHT = "app.tapzone.hilight";
    String PROP_APP_FLICK_BACKLIGHT_CONTROL = "app.screen.backlight.control.flick";
    String PROP_APP_BOOK_SORT_ORDER = "app.browser.sort.order";
    String PROP_APP_DICTIONARY = "app.dictionary.current";
    String PROP_APP_SELECTION_ACTION = "app.selection.action";
    String PROP_APP_MULTI_SELECTION_ACTION = "app.multiselection.action";
    String PROP_APP_SELECTION_PERSIST = "app.selection.persist";

    String PROP_APP_HIGHLIGHT_BOOKMARKS = "crengine.highlight.bookmarks";
    String PROP_HIGHLIGHT_SELECTION_COLOR = "crengine.highlight.selection.color";
    String PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT = "crengine.highlight.bookmarks.color.comment";
    String PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION = "crengine.highlight.bookmarks.color.correction";
    String PROP_APP_HIGHLIGHT_BOOKMARKS_DAY = "crengine.highlight.bookmarks.day";
    String PROP_HIGHLIGHT_SELECTION_COLOR_DAY = "crengine.highlight.selection.color.day";
    String PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_DAY = "crengine.highlight.bookmarks.color.comment.day";
    String PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_DAY = "crengine.highlight.bookmarks.color.correction.day";
    String PROP_APP_HIGHLIGHT_BOOKMARKS_NIGHT = "crengine.highlight.bookmarks.night";
    String PROP_HIGHLIGHT_SELECTION_COLOR_NIGHT = "crengine.highlight.selection.color.night";
    String PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_NIGHT = "crengine.highlight.bookmarks.color.comment.night";
    String PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_NIGHT = "crengine.highlight.bookmarks.color.correction.night";

    String PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS = "app.browser.hide.empty.folders";
    String PROP_APP_FILE_BROWSER_SIMPLE_MODE = "app.browser.simple.mode";

    String PROP_APP_SCREEN_UPDATE_MODE = "app.screen.update.mode";
    String PROP_APP_SCREEN_UPDATE_INTERVAL = "app.screen.update.interval";
    String PROP_APP_SECONDARY_TAP_ACTION_TYPE = "app.touch.secondary.action.type";
    String PROP_APP_GESTURE_PAGE_FLIPPING = "app.touch.gesture.page.flipping";


    String PROP_APP_THEME = "app.ui.theme";
    String PROP_APP_THEME_DAY = "app.ui.theme.day";
    String PROP_APP_THEME_NIGHT = "app.ui.theme.night";

    String PROP_APP_LOCALE = "app.locale.name";

    String PROP_APP_STARTUP_ACTION = "app.startup.action";

    String PROP_APP_PLUGIN_ENABLED = "app.plugin.enabled.litres";


    // available options for PROP_APP_SELECTION_ACTION setting
    int SELECTION_ACTION_TOOLBAR = 0;
    int SELECTION_ACTION_COPY = 1;
    int SELECTION_ACTION_DICTIONARY = 2;
    int SELECTION_ACTION_BOOKMARK = 3;
    int SELECTION_ACTION_FIND = 4;

    // available options for PROP_APP_SECONDARY_TAP_ACTION_TYPE setting
    int TAP_ACTION_TYPE_LONGPRESS = 0;
    int TAP_ACTION_TYPE_DOUBLE = 1;
    int TAP_ACTION_TYPE_SHORT = 2;

    // available options for PROP_APP_FLICK_BACKLIGHT_CONTROL setting
    int BACKLIGHT_CONTROL_FLICK_NONE = 0;
    int BACKLIGHT_CONTROL_FLICK_LEFT = 1;
    int BACKLIGHT_CONTROL_FLICK_RIGHT = 2;

    int APP_STARTUP_ACTION_LAST_BOOK = 0;
    int APP_STARTUP_ACTION_ROOT = 1;
    int APP_STARTUP_ACTION_RECENT_BOOKS = 2;
    int APP_STARTUP_ACTION_LAST_BOOK_FOLDER = 3;

    int VIEWER_STATUS_NONE = 0;
    int VIEWER_STATUS_TOP = 1;
    int VIEWER_STATUS_BOTTOM = 2;
    int VIEWER_STATUS_PAGE = 3;

    int VIEWER_TOOLBAR_NONE = 0;
    int VIEWER_TOOLBAR_TOP = 1;
    int VIEWER_TOOLBAR_BOTTOM = 2;
    int VIEWER_TOOLBAR_LEFT = 3;
    int VIEWER_TOOLBAR_RIGHT = 4;
    int VIEWER_TOOLBAR_SHORT_SIDE = 5;
    int VIEWER_TOOLBAR_LONG_SIDE = 6;


    enum Lang {
        DEFAULT("system", R.string.options_app_locale_system, R.raw.help_template_en),
        EN("en", R.string.options_app_locale_en, R.raw.help_template_en),
        DE("de", R.string.options_app_locale_de, 0),
        ES("es", R.string.options_app_locale_es, 0),
        FR("fr", R.string.options_app_locale_fr, 0),
        JA("ja", R.string.options_app_locale_ja, 0),
        RU("ru", R.string.options_app_locale_ru, R.raw.help_template_ru),
        UK("uk", R.string.options_app_locale_uk, R.raw.help_template_ru),
        BG("bg", R.string.options_app_locale_bg, 0),
        BY("by", R.string.options_app_locale_by, 0),
        SK("sk", R.string.options_app_locale_sk, 0),
        TR("tr", R.string.options_app_locale_tr, 0),
        LT("lt", R.string.options_app_locale_lt, 0),
        IT("it", R.string.options_app_locale_it, 0),
        HU("hu", R.string.options_app_locale_hu, R.raw.help_template_hu),
        NL("nl", R.string.options_app_locale_nl, 0),
        PL("pl", R.string.options_app_locale_pl, 0),
        PT("pt", R.string.options_app_locale_pt, 0),
        PT_BR("pt_BR", R.string.options_app_locale_pt_rbr, 0),
        CS("cs", R.string.options_app_locale_cs, 0),
        ZH_CN("zh_CN", R.string.options_app_locale_zh_cn, R.raw.help_template_zh_cn),
        ;

        public Locale getLocale() {
            return getLocale(code);
        }

        static public Locale getLocale(String code) {
            if (code.length() == 2)
                return new Locale(code);
            if (code.length() == 5)
                return new Locale(code.substring(0, 2), code.substring(3, 5));
            return null;
        }

        static public String getCode(Locale locale) {
            String country = locale.getCountry();
            if (country == null || country.length() == 0)
                return locale.getLanguage();
            return locale.getLanguage() + "_" + country;
        }

        static public Lang byCode(String code) {
            for (Lang lang : values())
                if (lang.code.equals(code))
                    return lang;
            if (code.length() > 2) {
                code = code.substring(0, 2);
                for (Lang lang : values())
                    if (lang.code.equals(code))
                        return lang;
            }
            Log.w("cr3", "language not found by code " + code);
            return DEFAULT;
        }

        Lang(String code, int nameResId, int helpFileResId) {
            this.code = code;
            this.nameId = nameResId;
            this.helpFileResId = helpFileResId;
        }

        public final String code;
        public final int nameId;
        public final int helpFileResId;
    }


    int MAX_PROFILES = 6;

    // settings which depend on profile
    String[] PROFILE_SETTINGS = {
            "background.*",
            PROP_NIGHT_MODE,
            "font.*",
            "crengine.page.*",
            PROP_FONT_SIZE,
            PROP_FALLBACK_FONT_FACE,
            PROP_INTERLINE_SPACE,
            PROP_STATUS_LINE,
            PROP_FOOTNOTES,
            "window.status.*",
            PROP_FLOATING_PUNCTUATION,
            PROP_LANDSCAPE_PAGES,
            PROP_HYPHENATION_DICT,
            "crengine.image.*",
            PROP_FORMAT_MIN_SPACE_CONDENSING_PERCENT,
            PROP_APP_FULLSCREEN,
            "app.screen.*",
            PROP_APP_DICTIONARY,
            PROP_APP_SELECTION_ACTION,
            PROP_APP_SELECTION_PERSIST,
            PROP_APP_HIGHLIGHT_BOOKMARKS + "*",
            PROP_HIGHLIGHT_SELECTION_COLOR + "*",
            PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT + "*",
            PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION + "*",

            "viewer.*",

            "app.key.*",
            "app.tapzone.*",
            PROP_APP_DOUBLE_TAP_SELECTION,
            "app.touch.*",

            "app.ui.theme*",
    };


}
