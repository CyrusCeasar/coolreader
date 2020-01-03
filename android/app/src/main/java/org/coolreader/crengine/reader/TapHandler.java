package org.coolreader.crengine.reader;

import android.view.MotionEvent;

import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.BookmarkEditDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.ImageInfo;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.crengine.Utils;

import java.io.File;

import static org.coolreader.crengine.reader.ZoneClickHelper.findTapZoneAction;
import static org.coolreader.crengine.reader.ZoneClickHelper.getTapZone;

public class TapHandler {
    public final int LONG_KEYPRESS_TIME = 900;
    public final int DOUBLE_CLICK_INTERVAL = 400;
    private final static int STATE_INITIAL = 0; // no events yet
    private final static int STATE_DOWN_1 = 1; // down first time
    private final static int STATE_SELECTION = 3; // selection is started
    private final static int STATE_FLIPPING = 4; // flipping is in progress
    private final static int STATE_WAIT_FOR_DOUBLE_CLICK = 5; // flipping is in progress
    private final static int STATE_DONE = 6; // done: no more tracking
    private final static int STATE_BRIGHTNESS = 7; // brightness change in progress

    private final static int EXPIRATION_TIME_MS = 180000;

    public TapHandler(ReaderView readerview) {
        mReaderView = readerview;
    }

    ReaderView mReaderView;

    int state = STATE_INITIAL;

    int start_x = 0;
    int start_y = 0;
    int width = 0;
    int height = 0;
    ReaderAction shortTapAction = ReaderAction.NONE;
    ReaderAction longTapAction = ReaderAction.NONE;
    ReaderAction doubleTapAction = ReaderAction.NONE;
    long firstDown;

    /// handle unexpected event for state: stop tracking
    private boolean unexpectedEvent() {
        cancel();
        return true; // ignore
    }

    public void checkExpiration() {
        if (state != STATE_INITIAL && Utils.timeInterval(firstDown) > EXPIRATION_TIME_MS)
            cancel();
    }

    /// cancel current action and reset touch tracking state
    public boolean cancel() {
        if (state == STATE_INITIAL)
            return true;
        switch (state) {
            case STATE_DOWN_1:
            case STATE_SELECTION:
                mReaderView.clearSelection();
                break;
            case STATE_FLIPPING:
                mReaderView.stopAnimation(-1, -1);
                break;
            case STATE_WAIT_FOR_DOUBLE_CLICK:
            case STATE_DONE:
            case STATE_BRIGHTNESS:
                mReaderView.stopBrightnessControl(-1, -1);
                break;
        }
        state = STATE_DONE;
        mReaderView.unhiliteTapZone();
        mReaderView.currentTapHandler = new TapHandler(mReaderView);
        return true;
    }

    /// perform action and reset touch tracking state
    private boolean performAction(final ReaderAction action, boolean checkForLinks) {
        state = STATE_DONE;

        mReaderView.currentTapHandler = new TapHandler(mReaderView);

        if (!checkForLinks) {
            mReaderView.onAction(action);
            return true;
        }

        // check link before executing action
        mReaderView.mEngine.execute(new Task() {
            String link;
            ImageInfo image;
            Bookmark bookmark;

            public void work() {
                image = new ImageInfo();
                image.bufWidth = mReaderView.internalDX;
                image.bufHeight = mReaderView.internalDY;
                image.bufDpi = mReaderView.mActivity.getDensityDpi();
                if (mReaderView.doc.checkImage(start_x, start_y, image)) {
                    return;
                }
                image = null;
                link = mReaderView.doc.checkLink(start_x, start_y, mReaderView.mActivity.getPalmTipPixels() / 2);
                if (link != null) {
                    if (link.startsWith("#")) {
                        mReaderView.doc.goLink(link);
                        mReaderView.drawPage();
                    }
                    return;
                }
                bookmark = mReaderView.doc.checkBookmark(start_x, start_y);
                if (bookmark != null && bookmark.getType() == Bookmark.TYPE_POSITION)
                    bookmark = null;
            }

            public void done() {
                if (bookmark != null)
                    bookmark = mReaderView.mBookInfo.findBookmark(bookmark);
                if (link == null && image == null && bookmark == null) {
                    mReaderView.onAction(action);
                } else if (image != null) {
                    mReaderView.startImageViewer(image);
                } else if (bookmark != null) {
                    BookmarkEditDialog dlg = new BookmarkEditDialog(mReaderView.mActivity, mReaderView, bookmark, false);
                    dlg.show();
                } else if (!link.startsWith("#")) {
                    if (link.startsWith("http://") || link.startsWith("https://")) {
                        mReaderView.mActivity.openURL(link);
                    } else {
                        // absolute path to file
                        FileInfo fi = new FileInfo(link);
                        if (fi.exists()) {
                            mReaderView.mActivity.loadDocument(fi);
                            return;
                        }
                        File baseDir = null;
                        if (mReaderView.mBookInfo != null && mReaderView.mBookInfo.getFileInfo() != null) {
                            if (!mReaderView.mBookInfo.getFileInfo().isArchive) {
                                // relatively to base directory
                                File f = new File(mReaderView.mBookInfo.getFileInfo().getBasePath());
                                baseDir = f.getParentFile();
                                String url = link;
                                while (baseDir != null && url != null && url.startsWith("../")) {
                                    baseDir = baseDir.getParentFile();
                                    url = url.substring(3);
                                }
                                if (baseDir != null && url != null && url.length() > 0) {
                                    fi = new FileInfo(baseDir.getAbsolutePath() + "/" + url);
                                    if (fi.exists()) {
                                        mReaderView.mActivity.loadDocument(fi);
                                        return;
                                    }
                                }
                            } else {
                                // from archive
                                fi = new FileInfo(mReaderView.mBookInfo.getFileInfo().getArchiveName() + FileInfo.ARC_SEPARATOR + link);
                                if (fi.exists()) {
                                    mReaderView.mActivity.loadDocument(fi);
                                    return;
                                }
                            }
                        }
                        mReaderView.mActivity.showToast("Cannot open link " + link);
                    }
                }
            }
        });
        return true;
    }

    private boolean startSelection() {
        state = STATE_SELECTION;
        // check link before executing action
        mReaderView.mEngine.execute(new Task() {
            ImageInfo image;
            Bookmark bookmark;

            public void work() {
                image = new ImageInfo();
                image.bufWidth = mReaderView.internalDX;
                image.bufHeight = mReaderView.internalDY;
                image.bufDpi = mReaderView.mActivity.getDensityDpi();
                if (!mReaderView.doc.checkImage(start_x, start_y, image))
                    image = null;
                bookmark = mReaderView.doc.checkBookmark(start_x, start_y);
                if (bookmark != null && bookmark.getType() == Bookmark.TYPE_POSITION)
                    bookmark = null;
            }

            public void done() {
                if (bookmark != null)
                    bookmark = mReaderView.mBookInfo.findBookmark(bookmark);
                if (image != null) {
                    cancel();
                    mReaderView.startImageViewer(image);
                } else if (bookmark != null) {
                    cancel();
                    BookmarkEditDialog dlg = new BookmarkEditDialog(mReaderView.mActivity, mReaderView, bookmark, false);
                    dlg.show();
                } else {
                    mReaderView.updateSelection(start_x, start_y, start_x, start_y, false);
                }
            }
        });
        return true;
    }

    private boolean trackDoubleTap() {
        state = STATE_WAIT_FOR_DOUBLE_CLICK;
        BackgroundThread.instance().postGUI(new Runnable() {
            @Override
            public void run() {
                if (mReaderView.currentTapHandler == TapHandler.this && state == STATE_WAIT_FOR_DOUBLE_CLICK)
                    performAction(shortTapAction, false);
            }
        }, DOUBLE_CLICK_INTERVAL);
        return true;
    }

    private boolean trackLongTap() {
        BackgroundThread.instance().postGUI(new Runnable() {
            @Override
            public void run() {
                if (mReaderView.currentTapHandler == TapHandler.this && state == STATE_DOWN_1) {
                    if (longTapAction == ReaderAction.START_SELECTION)
                        startSelection();
                    else
                        performAction(longTapAction, true);
                }
            }
        }, LONG_KEYPRESS_TIME);
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if ((DeviceInfo.getSDKLevel() >= 19) && mReaderView.mActivity.isFullscreen() && (event.getAction() == MotionEvent.ACTION_DOWN)) {
            if ((y < 30) || (y > (mReaderView.getSurface().getHeight() - 30)))
                return unexpectedEvent();
        }

        if (state == STATE_INITIAL && event.getAction() != MotionEvent.ACTION_DOWN)
            return unexpectedEvent(); // ignore unexpected event

        if (event.getAction() == MotionEvent.ACTION_UP) {
            long duration = Utils.timeInterval(firstDown);
            switch (state) {
                case STATE_DOWN_1:
                    if (mReaderView.hiliteTapZoneOnTap) {
                        mReaderView.hiliteTapZone(true, x, y, width, height);
                        mReaderView.scheduleUnhilite(LONG_KEYPRESS_TIME);
                    }
                    if (duration > LONG_KEYPRESS_TIME) {
                        if (longTapAction == ReaderAction.START_SELECTION)
                            return startSelection();
                        return performAction(longTapAction, true);
                    }
                    if (doubleTapAction.isNone())
                        return performAction(shortTapAction, false);
                    // start possible double tap tracking
                    return trackDoubleTap();
                case STATE_FLIPPING:
                    mReaderView.stopAnimation(x, y);
                    state = STATE_DONE;
                    return cancel();
                case STATE_BRIGHTNESS:
                    mReaderView.stopBrightnessControl(x, y);
                    state = STATE_DONE;
                    return cancel();
                case STATE_SELECTION:
                    // If the second tap is within a radius of the first tap point, assume the user is trying to double tap on the same point
                    if (start_x - x <= mReaderView.DOUBLE_TAP_RADIUS && x - start_x <= mReaderView.DOUBLE_TAP_RADIUS && y - start_y <= mReaderView.DOUBLE_TAP_RADIUS && start_y - y <= mReaderView.DOUBLE_TAP_RADIUS)
                        mReaderView.updateSelection(start_x, start_y, start_x, start_y, true);
                    else
                        mReaderView.updateSelection(start_x, start_y, x, y, true);
                    mReaderView.selectionModeActive = false;
                    state = STATE_DONE;
                    return cancel();
            }
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            switch (state) {
                case STATE_INITIAL:
                    start_x = x;
                    start_y = y;
                    width = mReaderView.surface.getWidth();
                    height = mReaderView.surface.getHeight();
                    int zone = getTapZone(x, y, width, height);
                    shortTapAction = findTapZoneAction(mReaderView.doubleTapSelectionEnabled, mReaderView.mSettings, mReaderView.secondaryTapActionType, zone, mReaderView.TAP_ACTION_TYPE_SHORT);
                    longTapAction = findTapZoneAction(mReaderView.doubleTapSelectionEnabled, mReaderView.mSettings, mReaderView.secondaryTapActionType, zone, mReaderView.TAP_ACTION_TYPE_LONGPRESS);
                    doubleTapAction = findTapZoneAction(mReaderView.doubleTapSelectionEnabled, mReaderView.mSettings, mReaderView.secondaryTapActionType, zone, mReaderView.TAP_ACTION_TYPE_DOUBLE);
                    firstDown = Utils.timeStamp();
                    if (mReaderView.selectionModeActive) {
                        startSelection();
                    } else {
                        state = STATE_DOWN_1;
                        trackLongTap();
                    }
                    return true;
                case STATE_DOWN_1:
                case STATE_BRIGHTNESS:
                case STATE_FLIPPING:
                case STATE_SELECTION:
                    return unexpectedEvent();
                case STATE_WAIT_FOR_DOUBLE_CLICK:
                    if (doubleTapAction == ReaderAction.START_SELECTION)
                        return startSelection();
                    return performAction(doubleTapAction, true);
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int dx = x - start_x;
            int dy = y - start_y;
            int adx = dx > 0 ? dx : -dx;
            int ady = dy > 0 ? dy : -dy;
            int distance = adx + ady;
            int dragThreshold = mReaderView.mActivity.getPalmTipPixels();
            switch (state) {
                case STATE_DOWN_1:
                    if (distance < dragThreshold)
                        return true;
                    if (mReaderView.isBacklightControlFlick != mReaderView.BACKLIGHT_CONTROL_FLICK_NONE && ady > adx) {
                        // backlight control enabled
                        if (start_x < dragThreshold * 170 / 100 && mReaderView.isBacklightControlFlick == 1
                                || start_x > width - dragThreshold * 170 / 100 && mReaderView.isBacklightControlFlick == 2) {
                            // brightness
                            state = STATE_BRIGHTNESS;
                            mReaderView.startBrightnessControl(start_x, start_y);
                            return true;
                        }
                    }
                    boolean isPageMode = mReaderView.mSettings.getInt(mReaderView.PROP_PAGE_VIEW_MODE, 1) == 1;
                    int dir = isPageMode ? x - start_x : y - start_y;
                    if (mReaderView.gesturePageFlippingEnabled) {
                        if (mReaderView.pageFlipAnimationSpeedMs == 0) {
                            // no animation
                            return performAction(dir < 0 ? ReaderAction.PAGE_DOWN : ReaderAction.PAGE_UP, false);
                        }
                        mReaderView.startAnimation(start_x, start_y, width, height, x, y);
                        mReaderView.updateAnimation(x, y);
                        state = STATE_FLIPPING;
                    }
                    return true;
                case STATE_FLIPPING:
                    mReaderView.updateAnimation(x, y);
                    return true;
                case STATE_BRIGHTNESS:
                    mReaderView.updateBrightnessControl(x, y);
                    return true;
                case STATE_WAIT_FOR_DOUBLE_CLICK:
                    return true;
                case STATE_SELECTION:
                    mReaderView.updateSelection(start_x, start_y, x, y, false);
                    break;
            }

        } else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            return unexpectedEvent();
        }
        return true;
    }
}