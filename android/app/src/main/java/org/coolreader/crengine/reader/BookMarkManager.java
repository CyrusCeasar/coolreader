package org.coolreader.crengine.reader;

import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.ReaderActivity;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;

import java.util.concurrent.Callable;

public class BookMarkManager implements Settings {
    private final static int DEF_SAVE_POSITION_INTERVAL = 180000; // 3 minutes
    private int lastSavePositionTaskId = 0;
    Bookmark lastPositionBookmarkToSave = null;
    Bookmark lastSavedBookmark = null;

    private BookInfo mBookInfo;
    private Engine mEngine;
    private ReaderActivity mActivity;
    private ReaderView mReaderView;

    public BookMarkManager(BookInfo bookInfo,Engine engine,ReaderActivity activity,ReaderView readerView){
        mBookInfo = bookInfo;
        mEngine = engine;
        mActivity = activity;
        mReaderView = readerView;
    }

    public void highlightBookmarks() {
        BackgroundThread.ensureGUI();
        if (mBookInfo == null || !mReaderView.isBookLoaded())
            return;
        int count = mBookInfo.getBookmarkCount();
        final Bookmark[] list = (count > 0 && mReaderView.flgHighlightBookmarks) ? new Bookmark[count] : null;
        for (int i = 0; i < count && mReaderView.flgHighlightBookmarks; i++)
            list[i] = mBookInfo.getBookmark(i);
        mEngine.post(new Task() {
            public void work() throws Exception {
                mReaderView.doc.hilightBookmarks(list);
                mReaderView.invalidImages = true;
            }

            public void done() {
                if (mReaderView.surface.isShown())
                    mReaderView.drawPage(true);
            }
        });
    }

    public void goToBookmark(Bookmark bm) {
        BackgroundThread.ensureGUI();
        final String pos = bm.getStartPos();
        mEngine.execute(new Task() {
            public void work() {
                BackgroundThread.ensureBackground();
                mReaderView.doc.goToPosition(pos, true);
            }

            public void done() {
                BackgroundThread.ensureGUI();
                mReaderView.drawPage();
            }
        });
    }

    public boolean goToBookmark(final int shortcut) {
        BackgroundThread.ensureGUI();
        if (mBookInfo != null) {
            Bookmark bm = mBookInfo.findShortcutBookmark(shortcut);
            if (bm == null) {
                addBookmark(shortcut);
                return true;
            } else {
                // go to bookmark
                goToBookmark(bm);
                return false;
            }
        }
        return false;
    }

    public Bookmark removeBookmark(final Bookmark bookmark) {
        Bookmark removed = mBookInfo.removeBookmark(bookmark);
        if (removed != null) {
            if (removed.getId() != null) {
                mActivity.getDB().deleteBookmark(removed);
            }
            highlightBookmarks();
        }
        return removed;
    }

    public Bookmark updateBookmark(final Bookmark bookmark) {
        Bookmark bm = mBookInfo.updateBookmark(bookmark);
        if (bm != null) {
            scheduleSaveCurrentPositionBookmark(DEF_SAVE_POSITION_INTERVAL);
            highlightBookmarks();
        }
        return bm;
    }

    public void addBookmark(final Bookmark bookmark) {
        mBookInfo.addBookmark(bookmark);
        highlightBookmarks();
        scheduleSaveCurrentPositionBookmark(DEF_SAVE_POSITION_INTERVAL);
    }

    public void addBookmark(final int shortcut) {
        BackgroundThread.ensureGUI();
        // set bookmark instead
        mEngine.execute(new Task() {
            Bookmark bm;

            public void work() {
                BackgroundThread.ensureBackground();
                if (mBookInfo != null) {
                    bm = mReaderView.doc.getCurrentPageBookmark();
                    bm.setShortcut(shortcut);
                }
            }

            public void done() {
                if (mBookInfo != null && bm != null) {
                    if (shortcut == 0)
                        mBookInfo.addBookmark(bm);
                    else
                        mBookInfo.setShortcutBookmark(shortcut, bm);
                    mActivity.getDB().saveBookInfo(mBookInfo);
                    String s;
                    if (shortcut == 0)
                        s = mActivity.getString(R.string.toast_position_bookmark_is_set);
                    else {
                        s = mActivity.getString(R.string.toast_shortcut_bookmark_is_set);
                        s.replace("$1", String.valueOf(shortcut));
                    }
                    highlightBookmarks();
                    mActivity.showToast(s);
                    scheduleSaveCurrentPositionBookmark(DEF_SAVE_POSITION_INTERVAL);
                }
            }
        });
    }

    public void prepareCurrentPositionBookmark() {
        if (!mReaderView.mOpened)
            return;
        Bookmark bmk = mReaderView.doc.getCurrentPageBookmarkNoRender();
        if (bmk != null) {
            bmk.setTimeStamp(System.currentTimeMillis());
            bmk.setType(Bookmark.TYPE_LAST_POSITION);
            if (mBookInfo != null)
                mBookInfo.setLastPosition(bmk);
        }
        lastPositionBookmarkToSave = bmk;
    }

    public void saveCurrentPositionBookmark() {
        Bookmark bmk = lastPositionBookmarkToSave;
        if (bmk != null && mBookInfo != null && mReaderView.isBookLoaded()) {
            //setBookPosition();
            if (lastSavedBookmark == null || !lastSavedBookmark.getStartPos().equals(bmk.getStartPos())) {
                if (Services.getHistory() != null)
                    Services.getHistory().updateRecentDir();
                if (mActivity.getDB() != null) {
                    mActivity.getDB().saveBookInfo(mBookInfo);
                    mActivity.getDB().flush();
                }
                lastSavedBookmark = bmk;
            }
            lastPositionBookmarkToSave = null;
        }
    }

    public void scheduleSaveCurrentPositionBookmark(){
        scheduleSaveCurrentPositionBookmark(DEF_SAVE_POSITION_INTERVAL);
    }
    private void scheduleSaveCurrentPositionBookmark(final int delayMillis) {


        // GUI thread required
        BackgroundThread.instance().executeGUI(new Runnable() {
            @Override
            public void run() {
                final int mylastSavePositionTaskId = ++lastSavePositionTaskId;
                if (mReaderView.isBookLoaded() && mBookInfo != null) {
                    prepareCurrentPositionBookmark();
                    Bookmark bmk = lastPositionBookmarkToSave;
                    if (bmk == null)
                        return;
                    final BookInfo bookInfo = mBookInfo;
                    if (delayMillis <= 1) {
                        if (bookInfo != null && mActivity.getDB() != null) {
                            saveCurrentPositionBookmark();
                            Services.getHistory().updateBookAccess(bookInfo, mReaderView.getTimeElapsed());
                        }
                    } else {
                        BackgroundThread.instance().postGUI(new Runnable() {
                            @Override
                            public void run() {
                                if (mylastSavePositionTaskId == lastSavePositionTaskId) {
                                    if (bookInfo != null) {
                                        if (Services.getHistory() != null) {
                                            saveCurrentPositionBookmark();
                                            Services.getHistory().updateBookAccess(bookInfo, mReaderView.getTimeElapsed());
                                        }
                                    }
                                }
                            }
                        }, delayMillis);
                    }
                }
            }
        });

    }

    public Bookmark saveCurrentPositionBookmarkSync(final boolean saveToDB) {
        ++lastSavePositionTaskId;
        Bookmark bmk = BackgroundThread.instance().callBackground(new Callable<Bookmark>() {
            @Override
            public Bookmark call() throws Exception {
                if (!mReaderView.mOpened)
                    return null;
                return mReaderView.doc.getCurrentPageBookmark();
            }
        });
        if (bmk != null) {
            //setBookPosition();
            bmk.setTimeStamp(System.currentTimeMillis());
            bmk.setType(Bookmark.TYPE_LAST_POSITION);
            if (mBookInfo != null)
                mBookInfo.setLastPosition(bmk);
            if (saveToDB) {
                Services.getHistory().updateRecentDir();
                mActivity.getDB().saveBookInfo(mBookInfo);
                mActivity.getDB().flush();
            }
        }
        return bmk;
    }

    public void close(){
        scheduleSaveCurrentPositionBookmark(DEF_SAVE_POSITION_INTERVAL);
        lastSavedBookmark = null;
        lastPositionBookmarkToSave = null;
    }

}
