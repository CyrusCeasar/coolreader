package org.coolreader.crengine.reader;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import org.coolreader.R;
import org.coolreader.crengine.AboutDialog;
import org.coolreader.crengine.BackgroundTextureInfo;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.BookInfoDialog;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.BookmarkEditDialog;
import org.coolreader.crengine.DelayedExecutor;
import org.coolreader.crengine.DocView;
import org.coolreader.crengine.DocumentFormat;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.FindNextDlg;
import org.coolreader.crengine.HelpFileGenerator;
import org.coolreader.crengine.ImageInfo;
import org.coolreader.crengine.InputDialog;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.OptionsDialog;
import org.coolreader.crengine.PositionProperties;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.crengine.ReaderActivity;
import org.coolreader.crengine.ReaderCallback;
import org.coolreader.crengine.ReaderCommand;
import org.coolreader.crengine.Scanner;
import org.coolreader.crengine.SearchDlg;
import org.coolreader.crengine.Selection;
import org.coolreader.crengine.SelectionToolbarDlg;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.SwitchProfileDialog;
import org.coolreader.crengine.TOCDlg;
import org.coolreader.crengine.TOCItem;
import org.coolreader.crengine.Utils;
import org.coolreader.crengine.VMRuntimeHack;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import cn.cc.ereader.MainActivity;
import cn.cyrus.translater.base.ToastUtilKt;
import cn.cyrus.translater.base.uitls.DeviceUtilKt;


public class ReaderView implements android.view.SurfaceHolder.Callback, Settings, OnTouchListener, OnFocusChangeListener {

    public static final Logger log = L.create("rv", Log.VERBOSE);
    private static final Logger alog = L.create("ra", Log.WARN);

    public final ReaderSurface surface;
    public final BookMarkManager mBookMarkManager;

    public SurfaceView getSurface() {
        return surface;
    }

    DocView doc;


    public static final int PAGE_ANIMATION_NONE = 0;
    public static final int PAGE_ANIMATION_PAPER = 1;
    public static final int PAGE_ANIMATION_SLIDE = 2;
    public static final int PAGE_ANIMATION_SLIDE2 = 3;
    public static final int PAGE_ANIMATION_MAX = 3;

    // Double tap selections within this radius are are assumed to be attempts to select a single point
    public static final int DOUBLE_TAP_RADIUS = 60;


    public final ReaderActivity mActivity;
    public final Engine mEngine;

    public BookInfo mBookInfo;

    public Properties mSettings = new Properties();

    public Engine getEngine() {
        return mEngine;
    }

    public ReaderActivity getActivity() {
        return mActivity;
    }

    private int lastResizeTaskId = 0;

    public boolean isBookLoaded() {
        return mOpened;
    }

    public int getOrientation() {
        int angle = mSettings.getInt(PROP_APP_SCREEN_ORIENTATION, 0);
        if (angle == 4)
            angle = mActivity.getOrientationFromSensor();
        return angle;
    }





    private long statStartTime;
    private long statTimeElapsed;

    public void startStats() {
        if (statStartTime == 0) {
            statStartTime = android.os.SystemClock.uptimeMillis();
            log.d("stats: started reading");
        }
    }

    public void stopStats() {
        if (statStartTime > 0) {
            statTimeElapsed += android.os.SystemClock.uptimeMillis() - statStartTime;
            statStartTime = 0;
            log.d("stats: stopped reading");
        }
    }

    public long getTimeElapsed() {
        if (statStartTime > 0)
            return statTimeElapsed + android.os.SystemClock.uptimeMillis() - statStartTime;
        else
            return statTimeElapsed++;
    }

    public void setTimeElapsed(long timeElapsed) {
        statTimeElapsed = timeElapsed;
    }

    public void onAppPause() {
        stopTracking();

        mBookMarkManager.prepareCurrentPositionBookmark();
        mBookMarkManager.saveCurrentPositionBookmark();
        log.i("calling bookView.onPause()");
        surface.onPause();
    }

    private long lastAppResumeTs = 0;

    public void onAppResume() {
        lastAppResumeTs = System.currentTimeMillis();
        log.i("calling bookView.onResume()");
        surface.onResume();
    }


    private void stopTracking() {
        if (currentTapHandler != null)
            currentTapHandler.cancel();
    }

    private int nextUpdateId = 0;

    public void updateSelection(int startX, int startY, int endX, int endY, final boolean isUpdateEnd) {
        final Selection sel = new Selection();
        final int myId = ++nextUpdateId;
        sel.startX = startX;
        sel.startY = startY;
        sel.endX = endX;
        sel.endY = endY;
        mEngine.execute(new Task() {
            @Override
            public void work() {
                if (myId != nextUpdateId && !isUpdateEnd)
                    return;
                doc.updateSelection(sel);
                if (!sel.isEmpty()) {
                    invalidImages = true;
                    BitmapInfo bi = preparePageImage(0);
                    if (bi != null) {
                        surface.draw(true);
                    }
                }
            }

            @Override
            public void done() {
                if (isUpdateEnd) {
                    String text = sel.text;
                    if (text != null && text.length() > 0) {
                        onSelectionComplete(sel);
                    } else {
                        clearSelection();
                    }
                }
            }
        });
    }

    public static boolean isMultiSelection(Selection sel) {
        String str = sel.text;
        if (str != null) {
            for (int i = 0; i < str.length(); i++) {
                if (Character.isWhitespace(str.charAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private int mSelectionAction = SELECTION_ACTION_DICTIONARY;
    private int mMultiSelectionAction = SELECTION_ACTION_TOOLBAR;

    private void onSelectionComplete(Selection sel) {
        int iSelectionAction;
        iSelectionAction = isMultiSelection(sel) ? mMultiSelectionAction : mSelectionAction;

        switch (iSelectionAction) {
            case SELECTION_ACTION_TOOLBAR:
                SelectionToolbarDlg.showDialog(mActivity, ReaderView.this, sel);
                break;
            case SELECTION_ACTION_COPY:
                DeviceUtilKt.copyToClipboard(mActivity, sel.text);
                ToastUtilKt.showToast(mActivity, "coped", Toast.LENGTH_SHORT);
                clearSelection();
                break;
            case SELECTION_ACTION_DICTIONARY:
                mActivity.findInDictionary(sel.text);
                if (!getSettings().getBool(PROP_APP_SELECTION_PERSIST, false))
                    clearSelection();
                break;
            case SELECTION_ACTION_BOOKMARK:
                clearSelection();
                showNewBookmarkDialog(sel);
                break;
            case SELECTION_ACTION_FIND:
                clearSelection();
                showSearchDialog(sel.text);
                break;
            default:
                clearSelection();
                break;
        }

    }

    public void showNewBookmarkDialog(Selection sel) {
        if (mBookInfo == null)
            return;
        Bookmark bmk = new Bookmark();
        bmk.setType(Bookmark.TYPE_COMMENT);
        bmk.setPosText(sel.text);
        bmk.setStartPos(sel.startPos);
        bmk.setEndPos(sel.endPos);
        bmk.setPercent(sel.percent);
        bmk.setTitleText(sel.chapter);
        BookmarkEditDialog dlg = new BookmarkEditDialog(mActivity, this, bmk, true);
        dlg.show();
    }

    public void sendQuotationInEmail(Selection sel) {
        StringBuilder buf = new StringBuilder();
        if (mBookInfo.getFileInfo().authors != null)
            buf.append("|" + mBookInfo.getFileInfo().authors + "\n");
        if (mBookInfo.getFileInfo().title != null)
            buf.append("|" + mBookInfo.getFileInfo().title + "\n");
        if (sel.chapter != null && sel.chapter.length() > 0)
            buf.append("|" + sel.chapter + "\n");
        buf.append(sel.text + "\n");
        mActivity.sendBookFragment(mBookInfo, buf.toString());
    }


    public int isBacklightControlFlick = 1;
    public boolean doubleTapSelectionEnabled = true;
    public boolean gesturePageFlippingEnabled = true;
    public int secondaryTapActionType = TAP_ACTION_TYPE_LONGPRESS;
    public boolean isTouchScreenEnabled = true;

    public boolean selectionModeActive = false;

    public void toggleSelectionMode() {
        selectionModeActive = !selectionModeActive;
        mActivity.showToast(selectionModeActive ? R.string.action_toggle_selection_mode_on : R.string.action_toggle_selection_mode_off);
    }

    ImageViewer currentImageViewer;


    public void startImageViewer(ImageInfo image) {
        currentImageViewer = new ImageViewer(image, mActivity, this);
        drawPage();
    }


    private void stopImageViewer() {
        if (currentImageViewer != null)
            currentImageViewer.close();
    }

    public TapHandler currentTapHandler = null;




    private void showTOC() {
        BackgroundThread.ensureGUI();
        final ReaderView view = this;
        mEngine.post(new Task() {
            TOCItem toc;
            PositionProperties pos;

            public void work() {
                BackgroundThread.ensureBackground();
                toc = doc.getTOC();
                pos = doc.getPositionProps(null);
            }

            public void done() {
                BackgroundThread.ensureGUI();
                if (toc != null && pos != null) {
                    TOCDlg dlg = new TOCDlg(mActivity, view, toc, pos.pageNumber);
                    dlg.show();
                } else {
                    mActivity.showToast("No Table of Contents found");
                }
            }
        });
    }

    public void showSearchDialog(String initialText) {
        if (initialText != null && initialText.length() > 40)
            initialText = initialText.substring(0, 40);
        BackgroundThread.ensureGUI();
        SearchDlg dlg = new SearchDlg(mActivity, this, initialText);
        dlg.show();
    }

    public void findText(final String pattern, final boolean reverse, final boolean caseInsensitive) {
        BackgroundThread.ensureGUI();
        final ReaderView view = this;
        mEngine.execute(new Task() {
            public void work() throws Exception {
                BackgroundThread.ensureBackground();
                boolean res = doc.findText(pattern, 1, reverse ? 1 : 0, caseInsensitive ? 1 : 0);
                if (!res)
                    res = doc.findText(pattern, -1, reverse ? 1 : 0, caseInsensitive ? 1 : 0);
                if (!res) {
                    doc.clearSelection();
                    throw new Exception("pattern not found");
                }
            }

            public void done() {
                BackgroundThread.ensureGUI();
                drawPage();
                FindNextDlg.showDialog(mActivity, view, pattern, caseInsensitive);
            }

            public void fail(Exception e) {
                BackgroundThread.ensureGUI();
                mActivity.showToast("Pattern not found");
            }

        });
    }

    public void findNext(final String pattern, final boolean reverse, final boolean caseInsensitive) {
        BackgroundThread.ensureGUI();
        mEngine.execute(new Task() {
            public void work() throws Exception {
                BackgroundThread.ensureBackground();
                boolean res = doc.findText(pattern, 1, reverse ? 1 : 0, caseInsensitive ? 1 : 0);
                if (!res)
                    res = doc.findText(pattern, -1, reverse ? 1 : 0, caseInsensitive ? 1 : 0);
                if (!res) {
                    doc.clearSelection();
                    throw new Exception("pattern not found");
                }
            }

            public void done() {
                BackgroundThread.ensureGUI();
//				drawPage();
                drawPage(true);
            }
        });
    }

    public boolean flgHighlightBookmarks = false;

    public void clearSelection() {
        BackgroundThread.ensureGUI();
        if (mBookInfo == null || !isBookLoaded())
            return;
        mEngine.post(new Task() {
            public void work() throws Exception {
                doc.clearSelection();
                invalidImages = true;
            }

            public void done() {
                if (surface.isShown())
                    drawPage(true);
            }
        });
    }


    public void onAction(final ReaderAction action) {
        onAction(action, null);
    }

    private void onAction(final ReaderAction action, final Runnable onFinishHandler) {
        BackgroundThread.ensureGUI();
        if (action.cmd != ReaderCommand.DCMD_NONE)
            onCommand(action.cmd, action.param, onFinishHandler);
    }

    private void toggleDayNightMode() {
        Properties settings = getSettings();
        OptionsDialog.toggleDayNightMode(settings);
        //setSettings(settings, mContext.settings());
        mActivity.setSettings(settings, 60000, true);
        invalidImages = true;
    }


    public String getSetting(String name) {
        return mSettings.getProperty(name);
    }

    public void setSetting(String name, String value, boolean invalidateImages, boolean save, boolean apply) {
        mActivity.setSetting(name, value, apply);
        invalidImages = true;
    }

    public void setSetting(String name, String value) {
        setSetting(name, value, true, false, true);
    }

    public void saveSetting(String name, String value) {
        setSetting(name, value, true, true, true);
    }

    public void toggleScreenOrientation() {
        int orientation = mActivity.getScreenOrientation();
        orientation = (orientation == 0) ? 1 : 0;
        saveSetting(PROP_APP_SCREEN_ORIENTATION, String.valueOf(orientation));
        mActivity.setScreenOrientation(orientation);
    }

    public void toggleFullscreen() {
        boolean newBool = !mActivity.isFullscreen();
        String newValue = newBool ? "1" : "0";
        saveSetting(PROP_APP_FULLSCREEN, newValue);
        mActivity.setFullscreen(newBool);
    }

    public void showReadingPositionPopup() {
        if (mBookInfo == null)
            return;
        final StringBuilder buf = new StringBuilder();
//		if (mContext.isFullscreen()) {
        buf.append(Utils.formatTime(mActivity, System.currentTimeMillis()) + " ");
        if (mBatteryState >= 0)
            buf.append(" [" + mBatteryState + "%]\n");
//		}
        mEngine.execute(new Task() {
            Bookmark bm;

            @Override
            public void work() {
                bm = doc.getCurrentPageBookmark();
                if (bm != null) {
                    PositionProperties prop = doc.getPositionProps(bm.getStartPos());
                    if (prop.pageMode != 0) {
                        buf.append("" + (prop.pageNumber + 1) + " / " + prop.pageCount + "   ");
                    }
                    int percent = (int) (10000 * (long) prop.y / prop.fullHeight);
                    buf.append("" + (percent / 100) + "." + (percent % 100) + "%");

                    // Show chapter details if book has more than one chapter
                    TOCItem toc = doc.getTOC();
                    if (toc != null && toc.getChildCount() > 1) {
                        TOCItem chapter = toc.getChapterAtPage(prop.pageNumber);

                        String chapterName = chapter.getName();
                        if (chapterName != null && chapterName.length() > 30)
                            chapterName = chapterName.substring(0, 30) + "...";

                        TOCItem nextChapter = chapter.getNextChapter();
                        int iChapterEnd = (nextChapter != null) ? nextChapter.getPage() : prop.pageCount;

                        String chapterPos = null;
                        if (prop.pageMode != 0) {
                            int iChapterStart = chapter.getPage();
                            int iChapterLen = iChapterEnd - iChapterStart;
                            int iChapterPage = prop.pageNumber - iChapterStart + 1;

                            chapterPos = "  (" + iChapterPage + " / " + iChapterLen + ")";
                        }

                        if (chapterName != null && chapterName.length() > 0)
                            buf.append("\n" + chapterName);
                        if (chapterPos != null && chapterPos.length() > 0)
                            buf.append(chapterPos);
                    }
                }
            }

            public void done() {
                mActivity.showToast(buf.toString());
            }
        });
    }

    public void toggleTitlebar() {
        boolean newBool = "1".equals(getSetting(PROP_STATUS_LINE));
        String newValue = !newBool ? "1" : "0";
        mActivity.setSetting(PROP_STATUS_LINE, newValue, true);
    }

    public void toggleDocumentStyles() {
        if (mOpened && mBookInfo != null) {
            log.d("toggleDocumentStyles()");
            boolean disableInternalStyles = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG);
            disableInternalStyles = !disableInternalStyles;
            mBookInfo.getFileInfo().setFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG, disableInternalStyles);
            doEngineCommand(ReaderCommand.DCMD_SET_INTERNAL_STYLES, disableInternalStyles ? 0 : 1);
            doEngineCommand(ReaderCommand.DCMD_REQUEST_RENDER, 1);
            mActivity.getDB().saveBookInfo(mBookInfo);
        }
    }

    public void toggleEmbeddedFonts() {
        if (mOpened && mBookInfo != null) {
            log.d("toggleEmbeddedFonts()");
            boolean enableInternalFonts = mBookInfo.getFileInfo().getFlag(FileInfo.USE_DOCUMENT_FONTS_FLAG);
            enableInternalFonts = !enableInternalFonts;
            mBookInfo.getFileInfo().setFlag(FileInfo.USE_DOCUMENT_FONTS_FLAG, enableInternalFonts);
            doEngineCommand(ReaderCommand.DCMD_SET_DOC_FONTS, enableInternalFonts ? 1 : 0);
            doEngineCommand(ReaderCommand.DCMD_REQUEST_RENDER, 1);
            mActivity.getDB().saveBookInfo(mBookInfo);
        }
    }

    public boolean isTextAutoformatEnabled() {
        if (mOpened && mBookInfo != null) {
            boolean disableTextReflow = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_REFLOW_TXT_FILES_FLAG);
            return !disableTextReflow;
        }
        return true;
    }

    public boolean isTextFormat() {
        if (mOpened && mBookInfo != null) {
            DocumentFormat fmt = mBookInfo.getFileInfo().format;
            return fmt == DocumentFormat.TXT || fmt == DocumentFormat.HTML || fmt == DocumentFormat.PDB;
        }
        return false;
    }

    public boolean isFormatWithEmbeddedFonts() {
        if (mOpened && mBookInfo != null) {
            DocumentFormat fmt = mBookInfo.getFileInfo().format;
            return fmt == DocumentFormat.EPUB;
        }
        return false;
    }

    public void toggleTextFormat() {
        if (mOpened && mBookInfo != null) {
            log.d("toggleDocumentStyles()");
            if (!isTextFormat())
                return;
            boolean disableTextReflow = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_REFLOW_TXT_FILES_FLAG);
            disableTextReflow = !disableTextReflow;
            mBookInfo.getFileInfo().setFlag(FileInfo.DONT_REFLOW_TXT_FILES_FLAG, disableTextReflow);
            mActivity.getDB().saveBookInfo(mBookInfo);
            reloadDocument();
        }
    }

    public boolean getDocumentStylesEnabled() {
        if (mOpened && mBookInfo != null) {
            boolean flg = !mBookInfo.getFileInfo().getFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG);
            return flg;
        }
        return true;
    }

    public boolean getDocumentFontsEnabled() {
        if (mOpened && mBookInfo != null) {
            boolean flg = mBookInfo.getFileInfo().getFlag(FileInfo.USE_DOCUMENT_FONTS_FLAG);
            return flg;
        }
        return true;
    }

    static private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public void showBookInfo() {
        final ArrayList<String> items = new ArrayList<String>();
        items.add("section=section.system");
        items.add("system.version=Cool Reader " + mActivity.getVersion());
        items.add("system.battery=" + mBatteryState + "%");
        items.add("system.time=" + Utils.formatTime(mActivity, System.currentTimeMillis()));
        final BookInfo bi = mBookInfo;
        if (bi != null) {
            FileInfo fi = bi.getFileInfo();
            items.add("section=section.file");
            String fname = new File(fi.pathname).getName();
            items.add("file.name=" + fname);
            if (new File(fi.pathname).getParent() != null)
                items.add("file.path=" + new File(fi.pathname).getParent());
            items.add("file.size=" + fi.size);
            if (fi.arcname != null) {
                items.add("file.arcname=" + new File(fi.arcname).getName());
                if (new File(fi.arcname).getParent() != null)
                    items.add("file.arcpath=" + new File(fi.arcname).getParent());
                items.add("file.arcsize=" + fi.arcsize);
            }
            items.add("file.format=" + fi.format.name());
        }
        mEngine.execute(new Task() {
            Bookmark bm;

            @Override
            public void work() {
                bm = doc.getCurrentPageBookmark();
                if (bm != null) {
                    PositionProperties prop = doc.getPositionProps(bm.getStartPos());
                    items.add("section=section.position");
                    if (prop.pageMode != 0) {
                        items.add("position.page=" + (prop.pageNumber + 1) + " / " + prop.pageCount);
                    }
                    int percent = (int) (10000 * (long) prop.y / prop.fullHeight);
                    items.add("position.percent=" + (percent / 100) + "." + (percent % 100) + "%");
                    String chapter = bm.getTitleText();
                    if (chapter != null && chapter.length() > 100)
                        chapter = chapter.substring(0, 100) + "...";
                    items.add("position.chapter=" + chapter);
                }
            }

            public void done() {
                FileInfo fi = bi.getFileInfo();
                items.add("section=section.book");
                if (fi.authors != null || fi.title != null || fi.series != null) {
                    items.add("book.authors=" + fi.authors);
                    items.add("book.title=" + fi.title);
                    if (fi.series != null) {
                        String s = fi.series;
                        if (fi.seriesNumber > 0)
                            s = s + " #" + fi.seriesNumber;
                        items.add("book.series=" + s);
                    }
                }
                if (fi.language != null) {
                    items.add("book.language=" + fi.language);
                }
                BookInfoDialog dlg = new BookInfoDialog(mActivity, items);
                dlg.show();
            }
        });
    }


    private void navigateByHistory(final ReaderCommand cmd) {
        BackgroundThread.instance().postBackground(new Runnable() {
            @Override
            public void run() {
                final boolean res = doc.doCommand(cmd.nativeId, 0);
                BackgroundThread.instance().postGUI(new Runnable() {
                    @Override
                    public void run() {
                        if (res) {
                            // successful
                            drawPage();
                        } else {
                            // cannot navigate - no data on stack
                            if (cmd == ReaderCommand.DCMD_LINK_BACK) {
                                // TODO: exit from activity in some cases?
                                getActivity().finish();
/*								if (mContext.isPreviousFrameHome())
									mContext.showRootWindow();
								else
									mContext.showBrowser(!mContext.isBrowserCreated() ? getOpenedFileInfo() : null);*/
                            }
                        }
                    }
                });
            }
        });
    }

    public void onCommand(final ReaderCommand cmd, final int param, final Runnable onFinishHandler) {
        BackgroundThread.ensureGUI();
        log.i("On command " + cmd + (param != 0 ? " (" + param + ")" : " "));
        switch (cmd) {
            case DCMD_FILE_BROWSER_ROOT:
//			mContext.showRootWindow();
                getActivity().finish();
                break;
            case DCMD_ABOUT:
                AboutDialog dlg = new AboutDialog(getActivity());
                dlg.show();
                break;
            case DCMD_SWITCH_PROFILE:
                showSwitchProfileDialog();
                break;
            case DCMD_OPEN_PREVIOUS_BOOK:
                loadPreviousDocument(new Runnable() {
                    @Override
                    public void run() {
                        // do nothing
                    }
                });
                break;
            case DCMD_BOOK_INFO:
                showBookInfo();
                break;
            case DCMD_USER_MANUAL:
                showManual();
                break;
            case DCMD_TOGGLE_DOCUMENT_STYLES:
                toggleDocumentStyles();
                break;
            case DCMD_SHOW_HOME_SCREEN:
                mActivity.showHomeScreen();
                break;
            case DCMD_TOGGLE_ORIENTATION:
                toggleScreenOrientation();
                break;
            case DCMD_TOGGLE_FULLSCREEN:
                toggleFullscreen();
                break;
            case DCMD_TOGGLE_TITLEBAR:
                toggleTitlebar();
                break;
            case DCMD_SHOW_POSITION_INFO_POPUP:
                showReadingPositionPopup();
                break;
            case DCMD_TOGGLE_SELECTION_MODE:
                toggleSelectionMode();
                break;
            case DCMD_TOGGLE_TOUCH_SCREEN_LOCK:
                isTouchScreenEnabled = !isTouchScreenEnabled;
                if (isTouchScreenEnabled)
                    mActivity.showToast(R.string.action_touch_screen_enabled_toast);
                else
                    mActivity.showToast(R.string.action_touch_screen_disabled_toast);
                break;
            case DCMD_LINK_BACK:
            case DCMD_LINK_FORWARD:
                navigateByHistory(cmd);
                break;
            case DCMD_ZOOM_OUT:
                doEngineCommand(ReaderCommand.DCMD_ZOOM_OUT, param);
                syncViewSettings(getSettings(), true, true);
                break;
            case DCMD_ZOOM_IN:
                doEngineCommand(ReaderCommand.DCMD_ZOOM_IN, param);
                syncViewSettings(getSettings(), true, true);
                break;
            case DCMD_FONT_NEXT:
                switchFontFace(1);
                break;
            case DCMD_FONT_PREVIOUS:
                switchFontFace(-1);
                break;
            case DCMD_MOVE_BY_CHAPTER:
                doEngineCommand(cmd, param, onFinishHandler);
                drawPage();
                break;
            case DCMD_PAGEDOWN:
                if (param == 1)
                    animatePageFlip(1, onFinishHandler);
                else
                    doEngineCommand(cmd, param, onFinishHandler);
                break;
            case DCMD_PAGEUP:
                if (param == 1)
                    animatePageFlip(-1, onFinishHandler);
                else
                    doEngineCommand(cmd, param, onFinishHandler);
                break;
            case DCMD_BEGIN:
            case DCMD_END:
                doEngineCommand(cmd, param);
                break;
            case DCMD_RECENT_BOOKS_LIST:
//			mContext.showRecentBooks();
                break;
            case DCMD_SEARCH:
                showSearchDialog(null);
                break;
            case DCMD_EXIT:
                mActivity.finish();
                break;
            case DCMD_BOOKMARKS:
                mActivity.showBookmarksDialog();
                break;
            case DCMD_GO_PERCENT_DIALOG:
                showGoToPercentDialog();
                break;
            case DCMD_GO_PAGE_DIALOG:
                showGoToPageDialog();
                break;
            case DCMD_TOC_DIALOG:
                showTOC();
                break;
            case DCMD_FILE_BROWSER:
//			mContext.showBrowser(!mContext.isBrowserCreated() ? getOpenedFileInfo() : null);
                break;
            case DCMD_CURRENT_BOOK_DIRECTORY:
//			mContext.showBrowser(getOpenedFileInfo());
                break;
            case DCMD_OPTIONS_DIALOG:
//			mContext.showOptionsDialog(OptionsDialog.Mode.READER);
                break;
            case DCMD_READER_MENU:
                mActivity.showReaderMenu();
                break;
            case DCMD_TOGGLE_DAY_NIGHT_MODE:
                toggleDayNightMode();
                break;
            default:
                // do nothing
                break;
        }
    }


    public void doEngineCommand(final ReaderCommand cmd, final int param) {
        doEngineCommand(cmd, param, null);
    }

    public void doEngineCommand(final ReaderCommand cmd, final int param, final Runnable doneHandler) {
        BackgroundThread.ensureGUI();
        log.d("doCommand(" + cmd + ", " + param + ")");
        mEngine.post(new Task() {
            boolean res;
            boolean isMoveCommand;

            public void work() {
                BackgroundThread.ensureBackground();
                res = doc.doCommand(cmd.nativeId, param);
                switch (cmd) {
                    case DCMD_BEGIN:
                    case DCMD_LINEUP:
                    case DCMD_PAGEUP:
                    case DCMD_PAGEDOWN:
                    case DCMD_LINEDOWN:
                    case DCMD_LINK_FORWARD:
                    case DCMD_LINK_BACK:
                    case DCMD_LINK_NEXT:
                    case DCMD_LINK_PREV:
                    case DCMD_LINK_GO:
                    case DCMD_END:
                    case DCMD_GO_POS:
                    case DCMD_GO_PAGE:
                    case DCMD_MOVE_BY_CHAPTER:
                    case DCMD_GO_SCROLL_POS:
                    case DCMD_LINK_FIRST:
                    case DCMD_SCROLL_BY:
                        isMoveCommand = true;
                        break;
                    default:
                        // do nothing
                        break;
                }
                if (isMoveCommand)
                    updateCurrentPositionStatus();
            }

            public void done() {
                if (res) {
                    invalidImages = true;
                    drawPage(doneHandler, false);
                }
                if (isMoveCommand)
                    mBookMarkManager.scheduleSaveCurrentPositionBookmark();
            }
        });
    }

    // update book and position info in status bar
    public void updateCurrentPositionStatus() {
        if (mBookInfo == null)
            return;
        // in background thread
        final FileInfo fileInfo = mBookInfo.getFileInfo();
        if (fileInfo == null)
            return;
        final Bookmark bmk = doc != null ? doc.getCurrentPageBookmark() : null;
        final PositionProperties props = bmk != null ? doc.getPositionProps(bmk.getStartPos()) : null;
        if (props != null) BackgroundThread.instance().postGUI(new Runnable() {
            @Override
            public void run() {
                mActivity.updateCurrentPositionStatus(fileInfo, bmk, props);


            }
        });
    }

    public void doCommandFromBackgroundThread(final ReaderCommand cmd, final int param) {
        log.d("doCommandFromBackgroundThread(" + cmd + ", " + param + ")");
        BackgroundThread.ensureBackground();
        boolean res = doc.doCommand(cmd.nativeId, param);
        if (res) {
            BackgroundThread.instance().executeGUI(new Runnable() {
                public void run() {
                    drawPage();
                }
            });
        }
    }

    public volatile boolean mInitialized = false;
    public volatile boolean mOpened = false;

    //private File historyFile;

    private void updateLoadedBookInfo() {
        BackgroundThread.ensureBackground();
        // get title, authors, etc.
        doc.updateBookInfo(mBookInfo);
        updateCurrentPositionStatus();
        // check whether current book properties updated on another devices
        // TODO: fix and reenable
        //syncUpdater.syncExternalChanges(mBookInfo);
    }

    private void applySettings(Properties props) {
        props = new Properties(props); // make a copy
        props.remove(PROP_TXT_OPTION_PREFORMATTED);
        props.remove(PROP_EMBEDDED_STYLES);
        props.remove(PROP_EMBEDDED_FONTS);
        BackgroundThread.ensureBackground();
        log.v("applySettings()");
        boolean isFullScreen = props.getBool(PROP_APP_FULLSCREEN, false);
        props.setBool(PROP_SHOW_BATTERY, isFullScreen);
        props.setBool(PROP_SHOW_TIME, isFullScreen);
        String backgroundImageId = props.getProperty(PROP_PAGE_BACKGROUND_IMAGE);
        int backgroundColor = props.getColor(PROP_BACKGROUND_COLOR, 0xFFFFFF);
        setBackgroundTexture(backgroundImageId, backgroundColor);
        props.setInt(PROP_STATUS_LINE, props.getInt(PROP_STATUS_LOCATION, VIEWER_STATUS_TOP) == VIEWER_STATUS_PAGE ? 0 : 1);

        int updMode = props.getInt(PROP_APP_SCREEN_UPDATE_MODE, 0);
        int updInterval = props.getInt(PROP_APP_SCREEN_UPDATE_INTERVAL, 10);


        doc.applySettings(props);
        //syncViewSettings(props, save, saveDelayed);
        drawPage();
    }

    public static boolean eq(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null)
            return true;
        if (obj1 == null || obj2 == null)
            return false;
        return obj1.equals(obj2);
    }

    public void saveSettings(Properties settings) {
        mActivity.setSettings(settings, 0, false);
    }

    /**
     * Read JNI view settings, update and save if changed
     */
    private void syncViewSettings(final Properties currSettings, final boolean save, final boolean saveDelayed) {
        mEngine.post(new Task() {
            Properties props;

            public void work() {
                BackgroundThread.ensureBackground();
                java.util.Properties internalProps = doc.getSettings();
                props = new Properties(internalProps);
            }

            public void done() {
                Properties changedSettings = props.diff(currSettings);
                for (Map.Entry<Object, Object> entry : changedSettings.entrySet()) {
                    currSettings.setProperty((String) entry.getKey(), (String) entry.getValue());
                }
                mSettings = currSettings;
                if (save) {
                    mActivity.setSettings(mSettings, saveDelayed ? 5000 : 0, false);
                } else {
                    mActivity.setSettings(mSettings, -1, false);
                }
            }
        });
    }

    public Properties getSettings() {
        return new Properties(mSettings);
    }

    static public int stringToInt(String value, int defValue) {
        if (value == null)
            return defValue;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private String getManualFileName() {
        Scanner s = Services.getScanner();
        if (s != null) {
            FileInfo fi = s.getDownloadDirectory();
            if (fi != null) {
                File bookDir = new File(fi.getPathName());
                return HelpFileGenerator.getHelpFileName(bookDir, mActivity.getCurrentLanguage()).getAbsolutePath();
            }
        }
        log.e("cannot get manual file name!");
        return null;
    }

    private File generateManual() {
        HelpFileGenerator generator = new HelpFileGenerator(mActivity, mEngine, getSettings(), mActivity.getCurrentLanguage());
        FileInfo downloadDir = Services.getScanner().getDownloadDirectory();
        File bookDir;
        if (downloadDir != null)
            bookDir = new File(Services.getScanner().getDownloadDirectory().getPathName());
        else {
            log.e("cannot download directory file name!");
            bookDir = new File("/tmp/");
        }
        int settingsHash = generator.getSettingsHash();
        String helpFileContentId = mActivity.getCurrentLanguage() + settingsHash + "v" + mActivity.getVersion();
        String lastHelpFileContentId = mActivity.getLastGeneratedHelpFileSignature();
        File manual = generator.getHelpFileName(bookDir);
        if (!manual.exists() || lastHelpFileContentId == null || !lastHelpFileContentId.equals(helpFileContentId)) {
            log.d("Generating help file " + manual.getAbsolutePath());
            mActivity.setLastGeneratedHelpFileSignature(helpFileContentId);
            manual = generator.generateHelpFile(bookDir);
        }
        return manual;
    }

    /**
     * Generate help file (if necessary) and show it.
     *
     * @return true if opened successfully
     */
    public boolean showManual() {
        return loadDocument(getManualFileName(), new Runnable() {
            @Override
            public void run() {
                mActivity.showToast("Error while opening manual");
            }
        });
    }

    public boolean hiliteTapZoneOnTap = false;
    static private final int DEF_PAGE_FLIP_MS = 300;

    public void applyAppSetting(String key, String value) {
        boolean flg = "1".equals(value);
        if (key.equals(PROP_APP_TAP_ZONE_HILIGHT)) {
            hiliteTapZoneOnTap = flg;
        } else if (key.equals(PROP_APP_DOUBLE_TAP_SELECTION)) {
            doubleTapSelectionEnabled = flg;
        } else if (key.equals(PROP_APP_GESTURE_PAGE_FLIPPING)) {
            gesturePageFlippingEnabled = flg;
        } else if (key.equals(PROP_APP_SECONDARY_TAP_ACTION_TYPE)) {
            secondaryTapActionType = flg ? TAP_ACTION_TYPE_DOUBLE : TAP_ACTION_TYPE_LONGPRESS;
        } else if (key.equals(PROP_APP_FLICK_BACKLIGHT_CONTROL)) {
            isBacklightControlFlick = "1".equals(value) ? 1 : ("2".equals(value) ? 2 : 0);
        } else if (PROP_APP_HIGHLIGHT_BOOKMARKS.equals(key)) {
            flgHighlightBookmarks = !"0".equals(value);
            clearSelection();
        } else if (PROP_PAGE_ANIMATION.equals(key)) {
            try {
                int n = Integer.valueOf(value);
                if (n < 0 || n > PAGE_ANIMATION_MAX)
                    n = PAGE_ANIMATION_SLIDE2;
                pageFlipAnimationMode = n;
            } catch (Exception e) {
                // ignore
            }
            pageFlipAnimationSpeedMs = pageFlipAnimationMode != PAGE_ANIMATION_NONE ? DEF_PAGE_FLIP_MS : 0;
        } else if (PROP_APP_SELECTION_ACTION.equals(key)) {
            try {
        	/*	int n = Integer.valueOf(value);
        		mSelectionAction = n;*/
            } catch (Exception e) {
                // ignore
            }
        } else if (PROP_APP_MULTI_SELECTION_ACTION.equals(key)) {
            try {
                int n = Integer.valueOf(value);
                mMultiSelectionAction = n;
            } catch (Exception e) {
                // ignore
            }
        } else {
            //mContext.applyAppSetting(key, value);
        }
        //
    }

    public void setAppSettings(Properties newSettings, Properties oldSettings) {
        log.v("setAppSettings()"); //|| keyCode == KeyEvent.KEYCODE_DPAD_LEFT
        BackgroundThread.ensureGUI();
        if (oldSettings == null)
            oldSettings = mSettings;
        Properties changedSettings = newSettings.diff(oldSettings);
        for (Map.Entry<Object, Object> entry : changedSettings.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            applyAppSetting(key, value);
            if (PROP_APP_FULLSCREEN.equals(key)) {
                boolean flg = mSettings.getBool(PROP_APP_FULLSCREEN, false);
                newSettings.setBool(PROP_SHOW_BATTERY, flg);
                newSettings.setBool(PROP_SHOW_TIME, flg);
            } else if (PROP_PAGE_VIEW_MODE.equals(key)) {
                boolean flg = "1".equals(value);
            } else if (PROP_APP_SCREEN_ORIENTATION.equals(key)
                    || PROP_PAGE_ANIMATION.equals(key)
                    || PROP_CONTROLS_ENABLE_VOLUME_KEYS.equals(key)
                    || PROP_APP_SHOW_COVERPAGES.equals(key)
                    || PROP_APP_COVERPAGE_SIZE.equals(key)
                    || PROP_APP_SCREEN_BACKLIGHT.equals(key)
                    || PROP_APP_BOOK_PROPERTY_SCAN_ENABLED.equals(key)
                    || PROP_APP_SCREEN_BACKLIGHT_LOCK.equals(key)
                    || PROP_APP_TAP_ZONE_HILIGHT.equals(key)
                    || PROP_APP_DICTIONARY.equals(key)
                    || PROP_APP_DOUBLE_TAP_SELECTION.equals(key)
                    || PROP_APP_FLICK_BACKLIGHT_CONTROL.equals(key)
                    || PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS.equals(key)
                    || PROP_APP_SELECTION_ACTION.equals(key)
                    || PROP_APP_FILE_BROWSER_SIMPLE_MODE.equals(key)
                    || PROP_APP_GESTURE_PAGE_FLIPPING.equals(key)
                    || PROP_APP_HIGHLIGHT_BOOKMARKS.equals(key)
                    || PROP_HIGHLIGHT_SELECTION_COLOR.equals(key)
                    || PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT.equals(key)
                    || PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION.equals(key)
                // TODO: redesign all this mess!
            ) {
                newSettings.setProperty(key, value);
            } else if (PROP_HYPHENATION_DICT.equals(key)) {
                Engine.HyphDict dict = Engine.HyphDict.byCode(value);
                if (mEngine.setHyphenationDictionary(dict)) {
                    if (isBookLoaded()) {
                        String language = getBookInfo().getFileInfo().getLanguage();
                        mEngine.setHyphenationLanguage(language);
                        doEngineCommand(ReaderCommand.DCMD_REQUEST_RENDER, 0);
                        //drawPage();
                    }
                }
                newSettings.setProperty(key, value);
            }
        }
    }


    /**
     * Change settings.
     *
     * @param newSettings are new settings
     */
    public void updateSettings(Properties newSettings) {
        log.v("updateSettings() " + newSettings.toString());
        log.v("oldNightMode=" + mSettings.getProperty(PROP_NIGHT_MODE) + " newNightMode=" + newSettings.getProperty(PROP_NIGHT_MODE));
        BackgroundThread.ensureGUI();
        final Properties currSettings = new Properties(mSettings);
        setAppSettings(newSettings, currSettings);
        Properties changedSettings = newSettings.diff(currSettings);
        currSettings.setAll(changedSettings);
        mSettings = currSettings;
        BackgroundThread.instance().postBackground(new Runnable() {
            public void run() {
                applySettings(currSettings);
            }
        });
    }

    private void setBackgroundTexture(String textureId, int color) {
        BackgroundTextureInfo[] textures = mEngine.getAvailableTextures();
        for (BackgroundTextureInfo item : textures) {
            if (item.id.equals(textureId)) {
                setBackgroundTexture(item, color);
                return;
            }
        }
        setBackgroundTexture(Engine.NO_TEXTURE, color);
    }

    private void setBackgroundTexture(BackgroundTextureInfo texture, int color) {
        log.v("setBackgroundTexture(" + texture + ", " + color + ")");
        if (!currentBackgroundTexture.equals(texture) || currentBackgroundColor != color) {
            log.d("setBackgroundTexture( " + texture + " )");
            currentBackgroundColor = color;
            currentBackgroundTexture = texture;
            byte[] data = mEngine.getImageData(currentBackgroundTexture);
            doc.setPageBackgroundTexture(data, texture.tiled ? 1 : 0);
            currentBackgroundTextureTiled = texture.tiled;
            if (data != null && data.length > 0) {
                if (currentBackgroundTextureBitmap != null)
                    currentBackgroundTextureBitmap.recycle();
                try {
                    currentBackgroundTextureBitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length);
                } catch (Exception e) {
                    log.e("Exception while decoding image data", e);
                    currentBackgroundTextureBitmap = null;
                }
            } else {
                currentBackgroundTextureBitmap = null;
            }
        }
    }

    BackgroundTextureInfo currentBackgroundTexture = Engine.NO_TEXTURE;
    Bitmap currentBackgroundTextureBitmap = null;
    boolean currentBackgroundTextureTiled = false;
    int currentBackgroundColor = 0;

    class CreateViewTask extends Task {
        Properties props;

        public CreateViewTask(Properties props) {
            this.props = props;
            Properties oldSettings = new Properties(); // may be changed by setAppSettings
            setAppSettings(props, oldSettings);
            props.setAll(oldSettings);
            mSettings = props;
        }

        public void work() throws Exception {
            BackgroundThread.ensureBackground();
            log.d("CreateViewTask - in background thread");
            byte[] data = mEngine.getImageData(currentBackgroundTexture);
            doc.setPageBackgroundTexture(data, currentBackgroundTexture.tiled ? 1 : 0);

            String css = mEngine.loadResourceUtf8(R.raw.fb2);
            if (css != null && css.length() > 0)
                doc.setStylesheet(css);
            applySettings(props);
            mInitialized = true;
            log.i("CreateViewTask - finished");
        }

        public void done() {
            log.d("InitializationFinishedEvent");
            //BackgroundThread.ensureGUI();
            //setSettings(props, new Properties());
        }

        public void fail(Exception e) {
            log.e("CoolReaderActivity engine initialization failed. Exiting.", e);
            mEngine.fatalError("Failed to init CoolReaderActivity engine");
        }
    }

    public void closeIfOpened(final FileInfo fileInfo) {
        if (this.mBookInfo != null && this.mBookInfo.getFileInfo().pathname.equals(fileInfo.pathname) && mOpened) {
            close();
        }
    }

    public boolean reloadDocument() {
        if (this.mBookInfo != null && this.mBookInfo.getFileInfo() != null) {
            save(); // save current position
            mEngine.post(new LoadDocumentTask(this.mBookInfo, null));
            return true;
        }
        return false;
    }

    public boolean loadDocument(final FileInfo fileInfo, final Runnable errorHandler) {
        log.v("loadDocument(" + fileInfo.getPathName() + ")");
        if (this.mBookInfo != null && this.mBookInfo.getFileInfo().pathname.equals(fileInfo.pathname) && mOpened) {
            log.d("trying to load already opened document");
            mActivity.showReader();
            drawPage();
            return false;
        }
        Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), fileInfo, bookInfo -> {
            log.v("posting LoadDocument task to background thread");
            BackgroundThread.instance().postBackground(new Runnable() {
                @Override
                public void run() {
                    log.v("posting LoadDocument task to GUI thread");
                    BackgroundThread.instance().postGUI(new Runnable() {
                        @Override
                        public void run() {
                            log.v("synced posting LoadDocument task to GUI thread");
                            mEngine.post(new LoadDocumentTask(bookInfo, errorHandler));
                        }
                    });
                }
            });
        });
        return true;
    }

    /**
     * When current book is opened, switch to previous book.
     *
     * @param errorHandler
     * @return
     */
    public boolean loadPreviousDocument(final Runnable errorHandler) {
        BackgroundThread.ensureGUI();
        BookInfo bi = Services.getHistory().getPreviousBook();
        if (bi != null && bi.getFileInfo() != null) {
            save();
            log.i("loadPreviousDocument() is called, prevBookName = " + bi.getFileInfo().getPathName());
            return loadDocument(bi.getFileInfo().getPathName(), errorHandler);
        }
        errorHandler.run();
        return false;
    }

    public boolean loadDocument(String fileName, final Runnable errorHandler) {
        BackgroundThread.ensureGUI();
        save();
        log.i("loadDocument(" + fileName + ")");
        if (fileName == null) {
            log.v("loadDocument() : no filename specified");
            if (errorHandler != null)
                errorHandler.run();
            return false;
        }
        if ("@manual".equals(fileName)) {
            fileName = getManualFileName();
            log.i("Manual document: " + fileName);
        }
        String normalized = mEngine.getPathCorrector().normalize(fileName);
        if (normalized == null) {
            log.e("Trying to load book from non-standard path " + fileName);
            mActivity.showToast("Trying to load book from non-standard path " + fileName);
            hideProgress();
            if (errorHandler != null)
                errorHandler.run();
            return false;
        } else if (!normalized.equals(fileName)) {
            log.w("Filename normalized to " + normalized);
            fileName = normalized;
        }
        if (fileName.equals(getManualFileName())) {
            // ensure manual file is up to date
            if (generateManual() == null) {
                log.v("loadDocument() : no filename specified");
                if (errorHandler != null)
                    errorHandler.run();
                return false;
            }
        }
        BookInfo book = Services.getHistory().getBookInfo(fileName);
        if (book != null)
            log.v("loadDocument() : found book in history : " + book);
        FileInfo fi = null;
        if (book == null) {
            log.v("loadDocument() : book not found in history, looking for location directory");
            FileInfo dir = Services.getScanner().findParent(new FileInfo(fileName), Services.getScanner().getRoot());
            if (dir != null) {
                log.v("loadDocument() : document location found : " + dir);
                fi = dir.findItemByPathName(fileName);
                log.v("loadDocument() : item inside location : " + fi);
            }
            if (fi == null) {
                log.v("loadDocument() : no file item " + fileName + " found inside " + dir);
                if (errorHandler != null)
                    errorHandler.run();
                return false;
            }
            if (fi.isDirectory) {
                log.v("loadDocument() : is a directory, opening browser");
//				mContext.showBrowser(fi);
                return true;
            }
        } else {
            fi = book.getFileInfo();
            log.v("loadDocument() : item from history : " + fi);
        }
        return loadDocument(fi, errorHandler);
    }

    public BookInfo getBookInfo() {
        BackgroundThread.ensureGUI();
        return mBookInfo;
    }


    private int mBatteryState = 100;

    public void setBatteryState(int state) {
        if (state != mBatteryState) {
            log.i("Battery state changed: " + state);
            mBatteryState = state;

            drawPage();

        }
    }

    public int getBatteryState() {
        return mBatteryState;
    }

    private static final VMRuntimeHack runtime = new VMRuntimeHack();
    BitmapFactory factory = new BitmapFactory(runtime);


    public BitmapInfo mCurrentPageInfo;
    public BitmapInfo mNextPageInfo;

    /**
     * Prepare and cache page image.
     * Cache is represented by two slots: mCurrentPageInfo and mNextPageInfo.
     * If page already exists in cache, returns it (if current page requested,
     * ensures that it became stored as mCurrentPageInfo; if another page requested,
     * no mCurrentPageInfo/mNextPageInfo reordering made).
     *
     * @param offset is kind of page: 0==current, -1=previous, 1=next page
     * @return page image and properties, null if requested page is unavailable (e.g. requested next/prev page is out of document range)
     */
    public BitmapInfo preparePageImage(int offset) {
        BackgroundThread.ensureBackground();
        log.v("preparePageImage( " + offset + ")");

        if (invalidImages) {
            if (mCurrentPageInfo != null)
                mCurrentPageInfo.recycle();
            mCurrentPageInfo = null;
            if (mNextPageInfo != null)
                mNextPageInfo.recycle();
            mNextPageInfo = null;
            invalidImages = false;
        }

        if (internalDX == 0 || internalDY == 0) {
            if (requestedWidth > 0 && requestedHeight > 0) {
                internalDX = requestedWidth;
                internalDY = requestedHeight;
                doc.resize(internalDX, internalDY);
            } else {
                internalDX = surface.getWidth();
                internalDY = surface.getHeight();
                doc.resize(internalDX, internalDY);
            }

        }

        if (currentImageViewer != null)
            return currentImageViewer.prepareImage();

        PositionProperties currpos = doc.getPositionProps(null);

        boolean isPageView = currpos.pageMode != 0;

        BitmapInfo currposBitmap = null;
        if (mCurrentPageInfo != null && mCurrentPageInfo.position.equals(currpos) && mCurrentPageInfo.imageInfo == null)
            currposBitmap = mCurrentPageInfo;
        else if (mNextPageInfo != null && mNextPageInfo.position.equals(currpos) && mNextPageInfo.imageInfo == null)
            currposBitmap = mNextPageInfo;
        if (offset == 0) {
            // Current page requested
            if (currposBitmap != null) {
                if (mNextPageInfo == currposBitmap) {
                    // reorder pages
                    BitmapInfo tmp = mNextPageInfo;
                    mNextPageInfo = mCurrentPageInfo;
                    mCurrentPageInfo = tmp;
                }
                // found ready page image
                return mCurrentPageInfo;
            }
            if (mCurrentPageInfo != null) {
                mCurrentPageInfo.recycle();
                mCurrentPageInfo = null;
            }
            BitmapInfo bi = new BitmapInfo(factory);
            bi.position = currpos;
            bi.bitmap = factory.get(internalDX > 0 ? internalDX : requestedWidth,
                    internalDY > 0 ? internalDY : requestedHeight);
            doc.setBatteryState(mBatteryState);
            doc.getPageImage(bi.bitmap);
            mCurrentPageInfo = bi;
            //log.v("Prepared new current page image " + mCurrentPageInfo);
            return mCurrentPageInfo;
        }
        if (isPageView) {
            // PAGES: one of next or prev pages requested, offset is specified as param
            int cmd1 = offset > 0 ? ReaderCommand.DCMD_PAGEDOWN.nativeId : ReaderCommand.DCMD_PAGEUP.nativeId;
            int cmd2 = offset > 0 ? ReaderCommand.DCMD_PAGEUP.nativeId : ReaderCommand.DCMD_PAGEDOWN.nativeId;
            if (offset < 0)
                offset = -offset;
            if (doc.doCommand(cmd1, offset)) {
                // can move to next page
                PositionProperties nextpos = doc.getPositionProps(null);
                BitmapInfo nextposBitmap = null;
                if (mCurrentPageInfo != null && mCurrentPageInfo.position.equals(nextpos))
                    nextposBitmap = mCurrentPageInfo;
                else if (mNextPageInfo != null && mNextPageInfo.position.equals(nextpos))
                    nextposBitmap = mNextPageInfo;
                if (nextposBitmap == null) {
                    // existing image not found in cache, overriding mNextPageInfo
                    if (mNextPageInfo != null)
                        mNextPageInfo.recycle();
                    mNextPageInfo = null;
                    BitmapInfo bi = new BitmapInfo(factory);
                    bi.position = nextpos;
                    bi.bitmap = factory.get(internalDX, internalDY);
                    doc.setBatteryState(mBatteryState);
                    doc.getPageImage(bi.bitmap);
                    mNextPageInfo = bi;
                    nextposBitmap = bi;
                    //log.v("Prepared new current page image " + mNextPageInfo);
                }
                // return back to previous page
                doc.doCommand(cmd2, offset);
                return nextposBitmap;
            } else {
                // cannot move to page: out of document range
                return null;
            }
        } else {
            // SCROLL next or prev page requested, with pixel offset specified
            int y = currpos.y + offset;
            if (doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, y)) {
                PositionProperties nextpos = doc.getPositionProps(null);
                BitmapInfo nextposBitmap = null;
                if (mCurrentPageInfo != null && mCurrentPageInfo.position.equals(nextpos))
                    nextposBitmap = mCurrentPageInfo;
                else if (mNextPageInfo != null && mNextPageInfo.position.equals(nextpos))
                    nextposBitmap = mNextPageInfo;
                if (nextposBitmap == null) {
                    // existing image not found in cache, overriding mNextPageInfo
                    if (mNextPageInfo != null)
                        mNextPageInfo.recycle();
                    mNextPageInfo = null;
                    BitmapInfo bi = new BitmapInfo(factory);
                    bi.position = nextpos;
                    bi.bitmap = factory.get(internalDX, internalDY);
                    doc.setBatteryState(mBatteryState);
                    doc.getPageImage(bi.bitmap);
                    mNextPageInfo = bi;
                    nextposBitmap = bi;
                }
                // return back to prev position
                doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, currpos.y);
                return nextposBitmap;
            } else {
                return null;
            }
        }

    }

    private int lastDrawTaskId = 0;

    private class DrawPageTask extends Task {
        final int id;
        BitmapInfo bi;
        Runnable doneHandler;
        boolean isPartially;

        DrawPageTask(Runnable doneHandler, boolean isPartially) {
            this.id = ++lastDrawTaskId;
            this.doneHandler = doneHandler;
            this.isPartially = isPartially;
            cancelGc();
        }

        public void work() {
            BackgroundThread.ensureBackground();
            if (this.id != lastDrawTaskId) {
                log.d("skipping duplicate drawPage request");
                return;
            }
            nextHiliteId++;
            if (currentAnimation != null) {
                log.d("skipping drawPage request while scroll animation is in progress");
                return;
            }
            log.e("DrawPageTask.work(" + internalDX + "," + internalDY + ")");
            bi = preparePageImage(0);
            if (bi != null) {
                surface.draw(isPartially);
            }
        }

        @Override
        public void done() {
            BackgroundThread.ensureGUI();
            if (doneHandler != null)
                doneHandler.run();
            scheduleGc();
        }

        @Override
        public void fail(Exception e) {
            hideProgress();
        }
    }

    private int requestedWidth = 0;
    private int requestedHeight = 0;


    public void requestResize(int width, int height) {
        requestedWidth = width;
        requestedHeight = height;
        checkSize();
    }

    private void checkSize() {
        boolean changed = (requestedWidth != internalDX) || (requestedHeight != internalDY);
        if (!changed)
            return;
//		if (mIsOnFront || !mOpened) {
        log.d("checkSize() : calling resize");
        resize();
//		} else {
//			log.d("Skipping resize request");
//		}
    }

    private void resize() {
        final int thisId = ++lastResizeTaskId;
//	    if ( w<h && mContext.isLandscape() ) {
//	    	log.i("ignoring size change to portrait since landscape is set");
//	    	return;
//	    }
//		if ( mContext.isPaused() ) {
//			log.i("ignoring size change since activity is paused");
//			return;
//		}
        // update size with delay: chance to avoid extra unnecessary resizing

        Runnable task = new Runnable() {
            public void run() {
                if (thisId != lastResizeTaskId) {
                    log.d("skipping duplicate resize request in GUI thread");
                    return;
                }
                mEngine.post(new Task() {
                    public void work() {
                        BackgroundThread.ensureBackground();
                        if (thisId != lastResizeTaskId) {
                            log.d("skipping duplicate resize request");
                            return;
                        }
                        internalDX = requestedWidth;
                        internalDY = requestedHeight;
                        log.d("ResizeTask: resizeInternal(" + internalDX + "," + internalDY + ")");
                        doc.resize(internalDX, internalDY);
//	    		        if ( mOpened ) {
//	    					log.d("ResizeTask: done, drawing page");
//	    			        drawPage();
//	    		        }
                    }

                    public void done() {
                        clearImageCache();
                        drawPage(null, false);
                        //redraw();
                    }
                });
            }
        };

        long timeSinceLastResume = System.currentTimeMillis() - lastAppResumeTs;
        int delay = 300;

        if (timeSinceLastResume < 1000)
            delay = 1000;

        if (mOpened) {
            log.d("scheduling delayed resize task id=" + thisId + " for " + delay + " ms");
            BackgroundThread.instance().postGUI(task, delay);
        } else {
            log.d("executing resize without delay");
            task.run();
        }
    }

    int hackMemorySize = 0;

    // SurfaceView callbacks
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, final int width,
                               final int height) {
        log.i("surfaceChanged(" + width + ", " + height + ")");

        if (hackMemorySize <= 0) {
            hackMemorySize = width * height * 2;
            runtime.trackFree(hackMemorySize);
        }


        surface.invalidate();
        //if (!isProgressActive())
        surface.draw();
        //requestResize(width, height);
        //draw();
    }

    boolean mSurfaceCreated = false;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        log.i("surfaceCreated()");
        mSurfaceCreated = true;
        //draw();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        log.i("surfaceDestroyed()");
        mSurfaceCreated = false;
        if (hackMemorySize > 0) {
            runtime.trackAlloc(hackMemorySize);
            hackMemorySize = 0;
        }
    }


    public ViewAnimationControl currentAnimation = null;

    public int pageFlipAnimationSpeedMs = DEF_PAGE_FLIP_MS; // if 0 : no animation
    public int pageFlipAnimationMode = PAGE_ANIMATION_SLIDE2; //PAGE_ANIMATION_PAPER; // if 0 : no animation

    //	private void animatePageFlip( final int dir ) {
//		animatePageFlip(dir, null);
//	}
    private void animatePageFlip(final int dir, final Runnable onFinishHandler) {
        BackgroundThread.instance().executeBackground(new Runnable() {
            @Override
            public void run() {
                BackgroundThread.ensureBackground();
                if (currentAnimation == null) {
                    PositionProperties currPos = doc.getPositionProps(null);
                    if (currPos == null)
                        return;
                    if (mCurrentPageInfo == null)
                        return;
                    int w = currPos.pageWidth;
                    int h = currPos.pageHeight;
                    int dir2 = dir;
//					if ( currPos.pageMode==2 )
//						if ( dir2==1 )
//							dir2 = 2;
//						else if ( dir2==-1 )
//							dir2 = -2;
                    int speed = pageFlipAnimationSpeedMs;
                    if (onFinishHandler != null)
                        speed = pageFlipAnimationSpeedMs / 2;
                    if (currPos.pageMode != 0) {
                        int fromX = dir2 > 0 ? w : 0;
                        int toX = dir2 > 0 ? 0 : w;
                        new PageViewAnimation(ReaderView.this, fromX, w, dir2);
                        if (currentAnimation != null) {
                            if (currentAnimation != null) {
                                nextHiliteId++;
                                hiliteRect = null;
                                currentAnimation.update(toX, h / 2);
                                currentAnimation.move(speed, true);
                                currentAnimation.stop(-1, -1);
                            }
                            if (onFinishHandler != null)
                                BackgroundThread.instance().executeGUI(onFinishHandler);
                        }
                    }
                }
            }
        });
    }

    static private Rect tapZoneBounds(int startX, int startY, int maxX, int maxY) {
        if (startX < 0)
            startX = 0;
        if (startY < 0)
            startY = 0;
        if (startX > maxX)
            startX = maxX;
        if (startY > maxY)
            startY = maxY;
        int dx = (maxX + 2) / 3;
        int dy = (maxY + 2) / 3;
        int x0 = startX / dx * dx;
        int y0 = startY / dy * dy;
        return new Rect(x0, y0, x0 + dx, y0 + dy);
    }

    volatile private int nextHiliteId = 0;

    private final static int HILITE_RECT_ALPHA = 32;
    private Rect hiliteRect = null;

    public void unhiliteTapZone() {
        hiliteTapZone(false, 0, 0, surface.getWidth(), surface.getHeight());
    }

    public void hiliteTapZone(final boolean hilite, final int startX, final int startY, final int maxX, final int maxY) {
        alog.d("highliteTapZone(" + startX + ", " + startY + ")");
        final int myHiliteId = ++nextHiliteId;
        int txcolor = mSettings.getColor(PROP_FONT_COLOR, Color.BLACK);
        final int color = (txcolor & 0xFFFFFF) | (HILITE_RECT_ALPHA << 24);
        BackgroundThread.instance().executeBackground(new Runnable() {
            @Override
            public void run() {
                if (myHiliteId != nextHiliteId || (!hilite && hiliteRect == null))
                    return;
                BackgroundThread.ensureBackground();
                final BitmapInfo pageImage = preparePageImage(0);
                if (pageImage != null && pageImage.bitmap != null && pageImage.position != null) {
                    //PositionProperties currPos = pageImage.position;
                    final Rect rc = hilite ? tapZoneBounds(startX, startY, maxX, maxY) : hiliteRect;
                    if (hilite)
                        hiliteRect = rc;
                    else
                        hiliteRect = null;
                    if (rc != null)
                        surface.drawCallback(new DrawCanvasCallback() {
                            @Override
                            public void drawTo(Canvas canvas) {
                                if (mInitialized && mCurrentPageInfo != null) {
                                    log.d("onDraw() -- drawing page image");
                                    drawDimmedBitmap(canvas, mCurrentPageInfo.bitmap, rc, rc);
                                    if (hilite) {
                                        Paint p = new Paint();
                                        p.setColor(color);
//					    			if ( true ) {
                                        canvas.drawRect(new Rect(rc.left, rc.top, rc.right - 2, rc.top + 2), p);
                                        canvas.drawRect(new Rect(rc.left, rc.top + 2, rc.left + 2, rc.bottom - 2), p);
                                        canvas.drawRect(new Rect(rc.right - 2 - 2, rc.top + 2, rc.right - 2, rc.bottom - 2), p);
                                        canvas.drawRect(new Rect(rc.left + 2, rc.bottom - 2 - 2, rc.right - 2 - 2, rc.bottom - 2), p);
//					    			} else {
//					    				canvas.drawRect(rc, p);
//					    			}
                                    }
                                }
                            }

                        }, rc, false);
                }
            }

        });
    }

    public void scheduleUnhilite(int delay) {
        final int myHiliteId = nextHiliteId;
        BackgroundThread.instance().postGUI(new Runnable() {
            @Override
            public void run() {
                if (myHiliteId == nextHiliteId && hiliteRect != null)
                    unhiliteTapZone();
            }
        }, delay);
    }

    int currentBrightnessValueIndex = -1;

    public void startBrightnessControl(final int startX, final int startY) {
        currentBrightnessValueIndex = -1;
        updateBrightnessControl(startX, startY);
    }

    public void updateBrightnessControl(final int x, final int y) {
        int n = OptionsDialog.mBacklightLevels.length;
        int index = n - 1 - y * n / surface.getHeight();
        if (index < 0)
            index = 0;
        else if (index >= n)
            index = n - 1;
        if (index != currentBrightnessValueIndex) {
            currentBrightnessValueIndex = index;
            int newValue = OptionsDialog.mBacklightLevels[currentBrightnessValueIndex];
            mActivity.backlightControl.setScreenBacklightLevel(newValue);
        }

    }

    public void stopBrightnessControl(final int x, final int y) {
        if (currentBrightnessValueIndex >= 0) {
            if (x >= 0 && y >= 0) {
                updateBrightnessControl(x, y);
            }
            mSettings.setInt(PROP_APP_SCREEN_BACKLIGHT, OptionsDialog.mBacklightLevels[currentBrightnessValueIndex]);
            OptionsDialog.mBacklightLevelsTitles[0] = mActivity.getString(R.string.options_app_backlight_screen_default);
            if (showBrightnessFlickToast) {
                String s = OptionsDialog.mBacklightLevelsTitles[currentBrightnessValueIndex];
                mActivity.showToast(s);
            }
            saveSettings(mSettings);
            currentBrightnessValueIndex = -1;
        }
    }

    private static final boolean showBrightnessFlickToast = false;


    public void startAnimation(final int startX, final int startY, final int maxX, final int maxY, final int newX, final int newY) {
        alog.d("startAnimation(" + startX + ", " + startY + ")");
        BackgroundThread.instance().executeBackground(new Runnable() {
            @Override
            public void run() {
                BackgroundThread.ensureBackground();
                PositionProperties currPos = doc.getPositionProps(null);
                if (currPos != null && currPos.pageMode != 0) {
                    //int dir = startX > maxX/2 ? currPos.pageMode : -currPos.pageMode;
                    //int dir = startX > maxX/2 ? 1 : -1;
                    int dir = newX - startX < 0 ? 1 : -1;
                    int sx = startX;
//					if ( dir<0 )
//						sx = 0;
                    new PageViewAnimation(ReaderView.this, sx, maxX, dir);
                }
                if (currentAnimation != null) {
                    nextHiliteId++;
                    hiliteRect = null;
                }
            }
        });
    }

    private class AnimationUpdate {
        private int x;
        private int y;

        //ViewAnimationControl myAnimation;
        public void set(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public AnimationUpdate(int x, int y) {
            this.x = x;
            this.y = y;
            //this.myAnimation = currentAnimation;
            scheduleUpdate();
        }

        private void scheduleUpdate() {
            BackgroundThread.instance().postBackground(new Runnable() {
                @Override
                public void run() {
                    alog.d("updating(" + x + ", " + y + ")");
                    boolean animate = false;
                    synchronized (AnimationUpdate.class) {

                        if (currentAnimation != null && currentAnimationUpdate == AnimationUpdate.this) {
                            currentAnimationUpdate = null;
                            currentAnimation.update(x, y);
                            animate = true;
                        }
                    }
                    if (animate)
                        currentAnimation.animate();
                }
            });
        }

    }

    private AnimationUpdate currentAnimationUpdate;

    public void updateAnimation(final int x, final int y) {
        alog.d("updateAnimation(" + x + ", " + y + ")");
        synchronized (AnimationUpdate.class) {
            if (currentAnimationUpdate != null)
                currentAnimationUpdate.set(x, y);
            else
                currentAnimationUpdate = new AnimationUpdate(x, y);
        }
        try {
            // give a chance to background thread to process event faster
            Thread.sleep(0);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void stopAnimation(final int x, final int y) {
        alog.d("stopAnimation(" + x + ", " + y + ")");
        BackgroundThread.instance().executeBackground(new Runnable() {
            @Override
            public void run() {
                if (currentAnimation != null) {
                    currentAnimation.stop(x, y);
                }
            }

        });
    }

    DelayedExecutor animationScheduler = DelayedExecutor.createBackground("animation");

    public void scheduleAnimation() {
        animationScheduler.post(new Runnable() {
            @Override
            public void run() {
                if (currentAnimation != null) {
                    currentAnimation.animate();
                }
            }
        });
    }


    private int drawAnimationPos = 0;
    private Long[] drawAnimationStats = new Long[8];
    private long avgDrawAnimationDuration = 200;

    public long getAvgAnimationDrawDuration() {
        return avgDrawAnimationDuration;
    }

    public void updateAnimationDurationStats(long duration) {
        if (duration <= 0)
            duration = 1;
        else if (duration > 1000)
            return;
        int pos = drawAnimationPos + 1;
        if (pos >= drawAnimationStats.length)
            pos = 0;
        drawAnimationStats[pos] = duration;
        drawAnimationPos = pos;
        long sum = 0;
        int count = 0;
        for (Long item : drawAnimationStats) {
            if (item != null) {
                sum += item;
                count++;
            }
        }
        avgDrawAnimationDuration = sum / count;
    }

    void drawPage() {
        drawPage(null, false);
    }

    public void drawPage(boolean isPartially) {
        drawPage(null, isPartially);
    }

    private void drawPage(Runnable doneHandler, boolean isPartially) {
        if (!mInitialized || !mOpened)
            return;
        log.v("drawPage() : submitting DrawPageTask");
        mBookMarkManager.scheduleSaveCurrentPositionBookmark();
        mEngine.post(new DrawPageTask(doneHandler, isPartially));
    }

    int internalDX = 0;
    int internalDY = 0;

    private byte[] coverPageBytes = null;

    private void findCoverPage() {
        log.d("document is loaded succesfull, checking coverpage data");
        byte[] coverpageBytes = doc.getCoverPageData();
        if (coverpageBytes != null) {
            log.d("Found cover page data: " + coverpageBytes.length + " bytes");
            coverPageBytes = coverpageBytes;
        }
    }

    public int currentProgressPosition = 1;
    public int currentProgressTitle = R.string.progress_loading;

    private void showProgress(int position, int titleResource) {
        log.v("showProgress(" + position + ")");
        boolean first = currentProgressTitle == 0;
        if (currentProgressPosition != position || currentProgressTitle != titleResource) {
            currentProgressPosition = position;
            currentProgressTitle = titleResource;
            surface.draw(!first);
        }
    }

    private void hideProgress() {
        log.v("hideProgress()");
        if (currentProgressTitle != 0) {
            currentProgressPosition = -1;
            currentProgressTitle = 0;
            surface.draw(false);
        }
    }

    public boolean isProgressActive() {
        return currentProgressPosition > 0;
    }

    private class LoadDocumentTask extends Task {
        String filename;
        String path;
        Runnable errorHandler;
        String pos;
        int profileNumber;
        boolean disableInternalStyles;
        boolean disableTextAutoformat;
        Properties props;

        LoadDocumentTask(BookInfo bookInfo, Runnable errorHandler) {
            BackgroundThread.ensureGUI();
            mBookInfo = bookInfo;
            FileInfo fileInfo = bookInfo.getFileInfo();
            log.v("LoadDocumentTask for " + fileInfo);
            if (fileInfo.getTitle() == null) {
                // As a book 'should' have a title, no title means we should
                // retrieve the book metadata from the engine to get the
                // book language.
                // Is it OK to do this here???  Should we use isScanned?
                // Should we use another fileInfo flag or a new flag?
                mEngine.scanBookProperties(fileInfo);
            }
            String language = fileInfo.getLanguage();
            log.v("update hyphenation language: " + language + " for " + fileInfo.getTitle());
            mEngine.setHyphenationLanguage(language);
            this.filename = fileInfo.getPathName();
            this.path = fileInfo.arcname != null ? fileInfo.arcname : fileInfo.pathname;
            this.errorHandler = errorHandler;
            //FileInfo fileInfo = new FileInfo(filename);
            disableInternalStyles = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG);
            disableTextAutoformat = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_REFLOW_TXT_FILES_FLAG);
            profileNumber = mBookInfo.getFileInfo().getProfileId();
            Properties oldSettings = new Properties(mSettings);
            // TODO: enable storing of profile per book
            mActivity.setCurrentProfile(profileNumber);
            if (mBookInfo != null && mBookInfo.getLastPosition() != null)
                pos = mBookInfo.getLastPosition().getStartPos();
            log.v("LoadDocumentTask : book info " + mBookInfo);
            log.v("LoadDocumentTask : last position = " + pos);
            if (mBookInfo != null && mBookInfo.getLastPosition() != null)
                setTimeElapsed(mBookInfo.getLastPosition().getTimeElapsed());
            //mBitmap = null;
            //showProgress(1000, R.string.progress_loading);
            //draw();
            BackgroundThread.instance().postGUI(() -> surface.draw(false));
            //init();
            // close existing document
            log.v("LoadDocumentTask : closing current book");
            close();
            if (props != null) {
                setAppSettings(props, oldSettings);
                BackgroundThread.instance().postBackground(() -> {
                    log.v("LoadDocumentTask : switching current profile");
                    applySettings(props);
                    log.i("Switching done");
                });
            }
        }

        @Override
        public void work() throws IOException {
            BackgroundThread.ensureBackground();
            coverPageBytes = null;
            log.i("Loading document " + filename);
            doc.doCommand(ReaderCommand.DCMD_SET_INTERNAL_STYLES.nativeId, disableInternalStyles ? 0 : 1);
            doc.doCommand(ReaderCommand.DCMD_SET_TEXT_FORMAT.nativeId, disableTextAutoformat ? 0 : 1);
            boolean success = doc.loadDocument(filename);
            if (success) {
                log.v("loadDocumentInternal completed successfully");

                doc.requestRender();

                findCoverPage();
                log.v("requesting page image, to render");
                if (internalDX == 0 || internalDY == 0) {
                    internalDX = surface.getWidth();
                    internalDY = surface.getHeight();
                    log.d("LoadDocument task: no size defined, resizing using widget size");
                    doc.resize(internalDX, internalDY);
                }
                preparePageImage(0);
                log.v("updating loaded book info");
                updateLoadedBookInfo();
                log.i("Document " + filename + " is loaded successfully");
                if (pos != null) {
                    log.i("Restoring position : " + pos);
                    restorePositionBackground(pos);
                }
                MainActivity.Companion.dumpHeapAllocation();
            } else {
                log.e("Error occured while trying to load document " + filename);
                throw new IOException("Cannot read document");
            }
        }

        @Override
        public void done() {
            BackgroundThread.ensureGUI();
            log.d("LoadDocumentTask, GUI thread is finished successfully");
            if (Services.getHistory() != null) {
                Services.getHistory().updateBookAccess(mBookInfo, getTimeElapsed());
                if (mActivity.getDB() != null)
                    mActivity.getDB().saveBookInfo(mBookInfo);
                if (coverPageBytes != null && mBookInfo != null && mBookInfo.getFileInfo() != null) {
                    if (mBookInfo.getFileInfo().format.needCoverPageCaching()) {
                        // TODO: fix it
//		        		if (mContext.getBrowser() != null)
//		        			mContext.getBrowser().setCoverpageData(new FileInfo(mBookInfo.getFileInfo()), coverPageBytes);
                    }

                }

                mOpened = true;

                mBookMarkManager.highlightBookmarks();

                drawPage();
                BackgroundThread.instance().postGUI(new Runnable() {
                    public void run() {
                        mActivity.showReader();
                    }
                });
//		        mContext.setLastBook(filename);
            }
        }

        public void fail(Exception e) {
            BackgroundThread.ensureGUI();
            log.v("LoadDocumentTask failed for " + mBookInfo, e);
            Services.getHistory().removeBookInfo(mActivity.getDB(), mBookInfo.getFileInfo(), true, false);
            mBookInfo = null;
            log.d("LoadDocumentTask is finished with exception " + e.getMessage());
            mOpened = false;
            drawPage();
            hideProgress();
            mActivity.showToast("Error while loading document");
            if (errorHandler != null) {
                log.e("LoadDocumentTask: Calling error handler");
                errorHandler.run();
            }
        }
    }


    private void dimRect(Canvas canvas, Rect dst) {

        int alpha = dimmingAlpha;
        if (alpha != 255) {
            Paint p = new Paint();
            p.setColor((255 - alpha) << 24);
            canvas.drawRect(dst, p);
        }
    }

    public void drawDimmedBitmap(Canvas canvas, Bitmap bmp, Rect src, Rect dst) {
        canvas.drawBitmap(bmp, src, dst, null);
        dimRect(canvas, dst);
    }

    protected void drawPageBackground(Canvas canvas, Rect dst, int side) {
        Bitmap bmp = currentBackgroundTextureBitmap;
        if (bmp != null) {
            int h = bmp.getHeight();
            int w = bmp.getWidth();
            Rect src = new Rect(0, 0, w, h);
            if (currentBackgroundTextureTiled) {
                // TILED
                for (int x = 0; x < dst.width(); x += w) {
                    int ww = w;
                    if (x + ww > dst.width())
                        ww = dst.width() - x;
                    for (int y = 0; y < dst.height(); y += h) {
                        int hh = h;
                        if (y + hh > dst.height())
                            hh = dst.height() - y;
                        Rect d = new Rect(x, y, x + ww, y + hh);
                        Rect s = new Rect(0, 0, ww, hh);
                        drawDimmedBitmap(canvas, bmp, s, d);
                    }
                }
            } else {
                // STRETCHED
                if (side == VIEWER_TOOLBAR_LONG_SIDE)
                    side = canvas.getWidth() > canvas.getHeight() ? VIEWER_TOOLBAR_TOP : VIEWER_TOOLBAR_LEFT;
                else if (side == VIEWER_TOOLBAR_SHORT_SIDE)
                    side = canvas.getWidth() < canvas.getHeight() ? VIEWER_TOOLBAR_TOP : VIEWER_TOOLBAR_LEFT;
                switch (side) {
                    case VIEWER_TOOLBAR_LEFT: {
                        int d = dst.width() * dst.height() / h;
                        if (d > w)
                            d = w;
                        src.left = src.right - d;
                    }
                    break;
                    case VIEWER_TOOLBAR_RIGHT: {
                        int d = dst.width() * dst.height() / h;
                        if (d > w)
                            d = w;
                        src.right = src.left + d;
                    }
                    break;
                    case VIEWER_TOOLBAR_TOP: {
                        int d = dst.height() * dst.width() / w;
                        if (d > h)
                            d = h;
                        src.top = src.bottom - d;
                    }
                    break;
                    case VIEWER_TOOLBAR_BOTTOM: {
                        int d = dst.height() * dst.width() / w;
                        if (d > h)
                            d = h;
                        src.bottom = src.top + d;
                    }
                    break;
                }
                drawDimmedBitmap(canvas, bmp, src, dst);
            }
        } else {
            canvas.drawColor(currentBackgroundColor | 0xFF000000);
        }
    }

    protected void drawPageBackground(Canvas canvas) {
        Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawPageBackground(canvas, dst, VIEWER_TOOLBAR_NONE);
    }

    public class ToolbarBackgroundDrawable extends Drawable {
        private int location = VIEWER_TOOLBAR_NONE;
        private int alpha;

        public void setLocation(int location) {
            this.location = location;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
            try {
                drawPageBackground(canvas, dst, location);
            } catch (Exception e) {
                L.e("Exception in ToolbarBackgroundDrawable.draw", e);
            }
        }

        @Override
        public int getOpacity() {
            return 255 - alpha;
        }

        @Override
        public void setAlpha(int alpha) {
            this.alpha = alpha;

        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            // not supported
        }
    }

    public ToolbarBackgroundDrawable createToolbarBackgroundDrawable() {
        return new ToolbarBackgroundDrawable();
    }

    protected void doDrawProgress(Canvas canvas, int position, int titleResource) {
        log.v("doDrawProgress(" + position + ")");
        if (titleResource == 0)
            return;
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        int mins = (w < h ? w : h) * 7 / 10;
        int ph = mins / 20;
        int textColor = mSettings.getColor(PROP_FONT_COLOR, 0x000000);
        Rect rc = new Rect(w / 2 - mins / 2, h / 2 - ph / 2, w / 2 + mins / 2, h / 2 + ph / 2);

        Utils.drawFrame(canvas, rc, Utils.createSolidPaint(0xC0000000 | textColor));
        //canvas.drawRect(rc, createSolidPaint(0xFFC0C0A0));
        rc.left += 2;
        rc.right -= 2;
        rc.top += 2;
        rc.bottom -= 2;
        int x = rc.left + (rc.right - rc.left) * position / 10000;
        Rect rc1 = new Rect(rc);
        rc1.right = x;
        canvas.drawRect(rc1, Utils.createSolidPaint(0x80000000 | textColor));
        Paint textPaint = Utils.createSolidPaint(0xFF000000 | textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(22f);
        textPaint.setSubpixelText(true);
        canvas.drawText(String.valueOf(mActivity.getText(titleResource)), (rc.left + rc.right) / 2, rc1.top - 12, textPaint);
    }


    private int dimmingAlpha = 255; // no dimming

    public void setDimmingAlpha(int alpha) {
        if (alpha > 255)
            alpha = 255;
        if (alpha < 32)
            alpha = 32;
        if (dimmingAlpha != alpha) {
            dimmingAlpha = alpha;
            mEngine.execute(new Task() {
                @Override
                public void work() throws Exception {
                    surface.draw();
                }

            });
        }
    }

    private void restorePositionBackground(String pos) {
        BackgroundThread.ensureBackground();
        if (pos != null) {
            BackgroundThread.ensureBackground();
            doc.goToPosition(pos, false);
            preparePageImage(0);
            hideProgress();
            drawPage();
            updateCurrentPositionStatus();
        }
    }


    public interface PositionPropertiesCallback {
        void onPositionProperties(PositionProperties props, String positionText);
    }

    public void getCurrentPositionProperties(final PositionPropertiesCallback callback) {
        BackgroundThread.instance().postBackground(new Runnable() {
            @Override
            public void run() {
                final Bookmark bmk = (doc != null) ? doc.getCurrentPageBookmarkNoRender() : null;
                final PositionProperties props = (bmk != null) ? doc.getPositionProps(bmk.getStartPos()) : null;
                BackgroundThread.instance().postBackground(new Runnable() {
                    @Override
                    public void run() {
                        String posText = null;
                        if (props != null) {
                            int percent = (int) (10000 * (long) props.y / props.fullHeight);
                            String percentText = "" + (percent / 100) + "." + (percent % 10) + "%";
                            posText = "" + props.pageNumber + " / " + props.pageCount + " (" + percentText + ")";
                        }
                        callback.onPositionProperties(props, posText);
                    }
                });

            }
        });
    }


    public void save() {
        BackgroundThread.ensureGUI();
        if (isBookLoaded() && mBookInfo != null) {
            log.v("saving last immediately");
            log.d("bookmark count 1 = " + mBookInfo.getBookmarkCount());
            Services.getHistory().updateBookAccess(mBookInfo, getTimeElapsed());
            log.d("bookmark count 2 = " + mBookInfo.getBookmarkCount());
            mActivity.getDB().saveBookInfo(mBookInfo);
            log.d("bookmark count 3 = " + mBookInfo.getBookmarkCount());
            mActivity.getDB().flush();
        }
        //scheduleSaveCurrentPositionBookmark(0);
        //post( new SavePositionTask() );
    }

    public void close() {
        BackgroundThread.ensureGUI();
        log.i("ReaderView.close() is called");
        if (!mOpened)
            return;
        cancelSwapTask();
        stopImageViewer();
        save();
        //scheduleSaveCurrentPositionBookmark(0);
        //save();
        mEngine.post(new Task() {
            public void work() {
                BackgroundThread.ensureBackground();
                if (mOpened) {
                    mOpened = false;
                    log.i("ReaderView().close() : closing current document");
                    doc.doCommand(ReaderCommand.DCMD_CLOSE_BOOK.nativeId, 0);
                }
            }

            public void done() {
                BackgroundThread.ensureGUI();
                if (currentAnimation == null) {
                    if (mCurrentPageInfo != null) {
                        mCurrentPageInfo.recycle();
                        mCurrentPageInfo = null;
                    }
                    if (mNextPageInfo != null) {
                        mNextPageInfo.recycle();
                        mNextPageInfo = null;
                    }
                } else
                    invalidImages = true;
                factory.compact();
                mCurrentPageInfo = null;
            }
        });
    }

    public void destroy() {
        log.i("ReaderView.destroy() is called");
        if (mInitialized) {
            //close();
            BackgroundThread.instance().postBackground(new Runnable() {
                public void run() {
                    BackgroundThread.ensureBackground();
                    if (mInitialized) {
                        log.i("ReaderView.destroyInternal() calling");
                        doc.destroy();
                        mInitialized = false;
                        currentBackgroundTexture = Engine.NO_TEXTURE;
                    }
                }
            });
            //engine.waitTasksCompletion();
        }
    }

    private String getCSSForFormat(DocumentFormat fileFormat) {
        if (fileFormat == null)
            fileFormat = DocumentFormat.FB2;
        File[] dataDirs = Engine.getDataDirectories(null, false, false);
        String defaultCss = mEngine.loadResourceUtf8(fileFormat.getCSSResourceId());
        for (File dir : dataDirs) {
            File file = new File(dir, fileFormat.getCssName());
            if (file.exists()) {
                String css = Engine.loadFileUtf8(file);
                if (css != null) {
                    int p1 = css.indexOf("@import");
                    if (p1 < 0)
                        p1 = css.indexOf("@include");
                    int p2 = css.indexOf("\";");
                    if (p1 >= 0 && p2 >= 0 && p1 < p2) {
                        css = css.substring(0, p1) + "\n" + defaultCss + "\n" + css.substring(p2 + 2);
                    }
                    return css;
                }
            }
        }
        return defaultCss;
    }

    ReaderCallback readerCallback = new ReaderCallback() {

        public boolean OnExportProgress(int percent) {
            log.d("readerCallback.OnExportProgress " + percent);
            return true;
        }

        public void OnExternalLink(String url, String nodeXPath) {
        }

        public void OnFormatEnd() {
            log.d("readerCallback.OnFormatEnd");
            //mEngine.hideProgress();
            hideProgress();
            drawPage();
            scheduleSwapTask();
        }

        public boolean OnFormatProgress(final int percent) {
            log.d("readerCallback.OnFormatProgress " + percent);
            showProgress(percent * 4 / 10 + 5000, R.string.progress_formatting);
            return true;
        }

        public void OnFormatStart() {
            log.d("readerCallback.OnFormatStart ");
        }

        public void OnLoadFileEnd() {
            log.d("readerCallback.OnLoadFileEnd");
            if (internalDX == 0 && internalDY == 0) {
                internalDX = requestedWidth;
                internalDY = requestedHeight;
                log.d("OnLoadFileEnd: resizeInternal(" + internalDX + "," + internalDY + ")");
                doc.resize(internalDX, internalDY);
                hideProgress();
            }

        }

        public void OnLoadFileError(String message) {
            log.d("readerCallback.OnLoadFileError(" + message + ")");
        }

        public void OnLoadFileFirstPagesReady() {
            log.d("readerCallback.OnLoadFileFirstPagesReady");
        }

        public String OnLoadFileFormatDetected(final DocumentFormat fileFormat) {
            log.i("readerCallback.OnLoadFileFormatDetected " + fileFormat);
            if (fileFormat != null) {
                String s = getCSSForFormat(fileFormat);
                return s;
            }
            return null;
        }

        public boolean OnLoadFileProgress(final int percent) {
            BackgroundThread.ensureBackground();
            log.d("readerCallback.OnLoadFileProgress " + percent);
            showProgress(percent * 4 / 10 + 1000, R.string.progress_loading);
            return true;
        }

        public void OnLoadFileStart(String filename) {
            cancelSwapTask();
            BackgroundThread.ensureBackground();
            log.d("readerCallback.OnLoadFileStart " + filename);
        }

        /// Override to handle external links
        public void OnImageCacheClear() {
            //log.d("readerCallback.OnImageCacheClear");
            clearImageCache();
        }

        public boolean OnRequestReload() {
            //reloadDocument();
            return true;
        }

    };

    private volatile SwapToCacheTask currentSwapTask;

    private void scheduleSwapTask() {
        currentSwapTask = new SwapToCacheTask();
        currentSwapTask.reschedule();
    }

    private void cancelSwapTask() {
        currentSwapTask = null;
    }

    private class SwapToCacheTask extends Task {
        boolean isTimeout;
        long startTime;

        public SwapToCacheTask() {
            startTime = System.currentTimeMillis();
        }

        public void reschedule() {
            if (this != currentSwapTask)
                return;
            BackgroundThread.instance().postGUI(new Runnable() {
                @Override
                public void run() {
                    mEngine.post(SwapToCacheTask.this);
                }
            }, 2000);
        }

        @Override
        public void work() throws Exception {
            if (this != currentSwapTask)
                return;
            int res = doc.swapToCache();
            isTimeout = res == DocView.SWAP_TIMEOUT;
            long duration = System.currentTimeMillis() - startTime;
            if (!isTimeout) {
                log.i("swapToCacheInternal is finished with result " + res + " in " + duration + " ms");
            } else {
                log.d("swapToCacheInternal exited by TIMEOUT in " + duration + " ms: rescheduling");
            }
        }

        @Override
        public void done() {
            if (isTimeout)
                reschedule();
        }

    }

    public boolean invalidImages = true;

    public void clearImageCache() {
        BackgroundThread.instance().postBackground(new Runnable() {
            public void run() {
                invalidImages = true;
            }
        });
    }

    public void setStyleSheet(final String css) {
        BackgroundThread.ensureGUI();
        if (css != null && css.length() > 0) {
            mEngine.post(new Task() {
                public void work() {
                    doc.setStylesheet(css);
                }
            });
        }
    }

    public void goToPosition(int position) {
        BackgroundThread.ensureGUI();
        doEngineCommand(ReaderCommand.DCMD_GO_POS, position);
    }

    public void moveBy(final int delta) {
        BackgroundThread.ensureGUI();
        log.d("moveBy(" + delta + ")");
        mEngine.post(new Task() {
            public void work() {
                BackgroundThread.ensureBackground();
                doc.doCommand(ReaderCommand.DCMD_SCROLL_BY.nativeId, delta);
                mBookMarkManager.scheduleSaveCurrentPositionBookmark();
            }

            public void done() {
                drawPage();
            }
        });
    }

    public void goToPage(int pageNumber) {
        BackgroundThread.ensureGUI();
        doEngineCommand(ReaderCommand.DCMD_GO_PAGE, pageNumber - 1);
    }

    public void goToPercent(final int percent) {
        BackgroundThread.ensureGUI();
        if (percent >= 0 && percent <= 100)
            mEngine.post(new Task() {
                public void work() {
                    PositionProperties pos = doc.getPositionProps(null);
                    if (pos != null && pos.pageCount > 0) {
                        int pageNumber = pos.pageCount * percent / 100;
                        doCommandFromBackgroundThread(ReaderCommand.DCMD_GO_PAGE, pageNumber);
                    }
                }
            });
    }


    public void moveSelection(final ReaderCommand command, final int param, final MoveSelectionCallback callback) {
        mEngine.post(new Task() {
            private boolean res;
            private Selection selection = new Selection();

            @Override
            public void work() throws Exception {
                res = doc.moveSelection(selection, command.nativeId, param);
            }

            @Override
            public void done() {
                if (callback != null) {
                    clearImageCache();
                    surface.invalidate();
                    drawPage();
                    if (res)
                        callback.onNewSelection(selection);
                    else
                        callback.onFail();
                }
            }

            @Override
            public void fail(Exception e) {
                if (callback != null)
                    callback.onFail();
            }


        });
    }

    private void showSwitchProfileDialog() {
        SwitchProfileDialog dlg = new SwitchProfileDialog(mActivity, this);
        dlg.show();
    }


    public void setCurrentProfile(int profile) {
        if (mActivity.getCurrentProfile() == profile)
            return;
        if (mBookInfo != null && mBookInfo.getFileInfo() != null) {
            mBookInfo.getFileInfo().setProfileId(profile);
            mActivity.getDB().saveBookInfo(mBookInfo);
        }
        log.i("Apply new profile settings");
        mActivity.setCurrentProfile(profile);
    }

    private static final int GC_INTERVAL = 15000; // 15 seconds
    DelayedExecutor gcTask = DelayedExecutor.createGUI("gc");

    public void scheduleGc() {
        try {
            gcTask.postDelayed(new Runnable() {
                @Override
                public void run() {
                    log.v("Initiating garbage collection");
                    System.gc();
                }
            }, GC_INTERVAL);
        } catch (Exception e) {
            // ignore
        }
    }

    public void cancelGc() {
        try {
            gcTask.cancel();
        } catch (Exception e) {
            // ignore
        }
    }

    private void switchFontFace(int direction) {
        String currentFontFace = mSettings.getProperty(PROP_FONT_FACE, "");
        String[] mFontFaces = Engine.getFontFaceList();
        int index = 0;
        int countFaces = mFontFaces.length;
        for (int i = 0; i < countFaces; i++) {
            if (mFontFaces[i].equals(currentFontFace)) {
                index = i;
                break;
            }
        }
        index += direction;
        if (index < 0)
            index = countFaces - 1;
        else if (index >= countFaces)
            index = 0;
        saveSetting(PROP_FONT_FACE, mFontFaces[index]);
        syncViewSettings(getSettings(), true, true);
    }

    public void showInputDialog(final String title, final String prompt, final boolean isNumberEdit, final int minValue, final int maxValue, final int lastValue, final InputDialog.InputHandler handler) {
        BackgroundThread.instance().executeGUI(new Runnable() {
            @Override
            public void run() {
                final InputDialog dlg = new InputDialog(mActivity, title, prompt, isNumberEdit, minValue, maxValue, lastValue, handler);
                dlg.show();
            }

        });
    }

    public void showGoToPageDialog() {
        getCurrentPositionProperties(new PositionPropertiesCallback() {
            @Override
            public void onPositionProperties(final PositionProperties props, final String positionText) {
                if (props == null)
                    return;
                String pos = mActivity.getString(R.string.dlg_goto_current_position) + " " + positionText;
                String prompt = mActivity.getString(R.string.dlg_goto_input_page_number);
                showInputDialog(mActivity.getString(R.string.mi_goto_page), pos + "\n" + prompt, true,
                        1, props.pageCount, props.pageNumber,
                        new InputDialog.InputHandler() {
                            int pageNumber = 0;

                            @Override
                            public boolean validate(String s) {
                                pageNumber = Integer.valueOf(s);
                                return pageNumber > 0 && pageNumber <= props.pageCount;
                            }

                            @Override
                            public void onOk(String s) {
                                goToPage(pageNumber);
                            }

                            @Override
                            public void onCancel() {
                            }
                        });
            }
        });
    }

    public void showGoToPercentDialog() {
        getCurrentPositionProperties((props, positionText) -> {
            if (props == null)
                return;
            String pos = mActivity.getString(R.string.dlg_goto_current_position) + " " + positionText;
            String prompt = mActivity.getString(R.string.dlg_goto_input_percent);
            showInputDialog(mActivity.getString(R.string.mi_goto_percent), pos + "\n" + prompt, true,
                    0, 100, props.y * 100 / props.fullHeight,
                    new InputDialog.InputHandler() {
                        int percent = 0;

                        @Override
                        public boolean validate(String s) {
                            percent = Integer.valueOf(s);
                            return percent >= 0 && percent <= 100;
                        }

                        @Override
                        public void onOk(String s) {
                            goToPercent(percent);
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return onTouchEvent(event);
    }

    private boolean onTouchEvent(MotionEvent event) {

        if (!isTouchScreenEnabled) {
            return true;
        }
        if (event.getX() == 0 && event.getY() == 0)
            return true;
        mActivity.onUserActivity();

        if (currentImageViewer != null)
            return currentImageViewer.onTouchEvent(event);


        if (currentTapHandler == null)
            currentTapHandler = new TapHandler(this);
        currentTapHandler.checkExpiration();
        return currentTapHandler.onTouchEvent(event);
    }

    @Override
    public void onFocusChange(View arg0, boolean arg1) {
        stopTracking();
    }

    public void redraw() {
        BackgroundThread.instance().executeGUI(new Runnable() {
            @Override
            public void run() {
                surface.invalidate();
                invalidImages = true;
                //preparePageImage(0);
                surface.draw();
            }
        });
    }

    public ReaderView(ReaderActivity activity, Engine engine, Properties props) {
        //super(activity);
        log.i("Creating normal SurfaceView");
        surface = new ReaderSurface(activity, mSettings, this);

        surface.setOnTouchListener(this);
//		surface.setOnKeyListener(this);
        surface.setOnFocusChangeListener(this);
        doc = new DocView(Engine.lock);
        doc.setReaderCallback(readerCallback);
        SurfaceHolder holder = surface.getHolder();
        holder.addCallback(this);

        BackgroundThread.ensureGUI();
        this.mActivity = activity;
        this.mEngine = engine;
        surface.setFocusable(true);
        surface.setFocusableInTouchMode(true);

        BackgroundThread.instance().postBackground(new Runnable() {

            @Override
            public void run() {
                log.d("ReaderView - in background thread: calling createInternal()");
                doc.create();
                mInitialized = true;
            }

        });

        log.i("Posting create view task");
        mEngine.post(new CreateViewTask(props));
        mBookMarkManager = new BookMarkManager(mBookInfo, engine, mActivity, this);

    }

}
