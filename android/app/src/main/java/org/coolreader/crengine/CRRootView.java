package org.coolreader.crengine;

import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import org.coolreader.R;
import org.coolreader.crengine.CoverpageManager.CoverpageReadyListener;
import org.coolreader.crengine.filebrowser.FileBrowserActivity;
import org.coolreader.plugins.OnlineStorePluginManager;
import org.coolreader.plugins.OnlineStoreWrapper;
import org.coolreader.plugins.litres.LitresPlugin;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import cn.cc.ereader.MainActivity;
import cn.cyrus.translater.base.uitls.DeviceUtilKt;


public class CRRootView extends ViewGroup implements CoverpageReadyListener {

    public static final Logger log = L.create("cr");

    private final MainActivity mActivity;
    private ViewGroup mView;
    private RecyclerView mRecentBooksScroll;
    private RecyclerView mFilesystemScroll;
    private CoverpageManager mCoverpageManager;
    private int coverWidth = DeviceUtilKt.dp2px(80);
    private int coverHeight = DeviceUtilKt.dp2px(80);
    private BookInfo currentBook;
    private CoverpageReadyListener coverpageListener;

    public CRRootView(MainActivity activity) {
        super(activity);
        this.mActivity = activity;
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.mCoverpageManager = Services.getCoverpageManager();
        setFocusable(true);
        setFocusableInTouchMode(true);
        createViews();
    }


    private InterfaceTheme lastTheme;

    public void onThemeChange(InterfaceTheme theme) {
        if (lastTheme != theme) {
            lastTheme = theme;
            createViews();
        }
    }

    public void onClose() {
        this.mCoverpageManager.removeCoverpageReadyListener(coverpageListener);
        coverpageListener = null;
        super.onDetachedFromWindow();
    }

    private void setBookInfoItem(ViewGroup baseView, int viewId, String value) {
        TextView view = baseView.findViewById(viewId);
        if (view != null) {
            if (value != null && value.length() > 0) {
                view.setText(value);
            } else {
                view.setText("");
            }
        }
    }

    private void updateCurrentBook(BookInfo book) {
        currentBook = book;

        // set current book cover page

        ImageView cover = mView.findViewById(R.id.book_cover);
        if (currentBook != null) {
            FileInfo item = currentBook.getFileInfo();
            cover.setImageDrawable(mCoverpageManager.getCoverpageDrawableFor(mActivity.getDB(), item, coverWidth, coverHeight));
            cover.setTag(new CoverpageManager.ImageItem(item, coverWidth, coverHeight));

            setBookInfoItem(mView, R.id.lbl_book_author, Utils.formatAuthors(item.authors));
            setBookInfoItem(mView, R.id.lbl_book_title, currentBook.getFileInfo().title);
            setBookInfoItem(mView, R.id.lbl_book_series, Utils.formatSeries(item.series, item.seriesNumber));
            String state = Utils.formatReadingState(mActivity, item);
            state = state + " " + Utils.formatFileInfo(mActivity, item) + " ";
            if (Services.getHistory() != null)
                state = state + " " + Utils.formatLastPosition(mActivity, Services.getHistory().getLastPos(item));
            setBookInfoItem(mView, R.id.lbl_book_info, state);
        } else {
            log.w("No current book in history");
            cover.setImageDrawable(null);
            cover.setMinimumHeight(0);
            cover.setMinimumWidth(0);
            cover.setMaxHeight(0);
            cover.setMaxWidth(0);

            setBookInfoItem(mView, R.id.lbl_book_author, "");
            setBookInfoItem(mView, R.id.lbl_book_title, "No last book"); // TODO: i18n
            setBookInfoItem(mView, R.id.lbl_book_series, "");
        }
    }

    private final static int MAX_RECENT_BOOKS = 12;

    private void updateRecentBooks(ArrayList<BookInfo> books) {
        ArrayList<FileInfo> files = new ArrayList<>();
        for (int i = 1; i <= MAX_RECENT_BOOKS && i < books.size(); i++)
            files.add(books.get(i).getFileInfo());
        if (books.size() > MAX_RECENT_BOOKS && Services.getScanner() != null)
            files.add(Services.getScanner().createRecentRoot());
        mRecentBooksScroll.setAdapter(new BaseQuickAdapter<FileInfo, BaseViewHolder>(R.layout.root_item_recent_book, files) {

            @Override
            protected void convert(@NonNull BaseViewHolder helper, FileInfo item) {
                final View view = helper.itemView;
                ImageView cover = helper.getView(R.id.book_cover);
                TextView label = helper.getView(R.id.book_name);
                if (item.isRecentDir()) {
                    cover.setImageResource(R.drawable.cr3_button_next);
                    if (label != null) {
                        label.setText("More...");
                    }
                    view.setOnClickListener(v -> mActivity.showRecentBooks());
                } else {
                    cover.setMinimumWidth(coverWidth);
                    cover.setTag(new CoverpageManager.ImageItem(item, coverWidth, coverHeight));
                    cover.setImageDrawable(mCoverpageManager.getCoverpageDrawableFor(mActivity.getDB(), item, coverWidth, coverHeight));
                    if (label != null) {
                        String title = item.title;
                        String authors = Utils.formatAuthors(item.authors);
                        String s = item.getFileNameToDisplay();
                        if (!Utils.empty(title) && !Utils.empty(authors))
                            s = title + " - " + authors;
                        else if (!Utils.empty(title))
                            s = title;
                        else if (!Utils.empty(authors))
                            s = authors;
                        label.setText(s != null ? s : "");
                    }
                    view.setOnClickListener(v -> ReaderActivity.Companion.loadDocument(getContext(), item.pathname));
                    view.setOnLongClickListener(v -> {
                                mActivity.askConfirmation(R.string.win_title_confirm_book_delete, () -> {
                                    FileInfo file = Services.getScanner().findFileInTree(item);
                                    if (file == null)
                                        file = item;
                                    if (file.deleteFile()) {
                                        Services.getHistory().removeBookInfo(mActivity.getDB(), file, true, true);
                                    }
                                });
                                return true;
                            }
                    );
                }
            }
        });

    }

    public void refreshRecentBooks() {
        BackgroundThread.instance().postGUI(() -> {
            if (Services.getHistory() != null && mActivity.getDB() != null)
                Services.getHistory().getOrLoadRecentBooks(mActivity.getDB(), bookList -> {
                    updateCurrentBook(bookList != null && bookList.size() > 0 ? bookList.get(0) : null);
                    updateRecentBooks(bookList);
                });
        });
    }



    public void refreshFileSystemFolders() {
        ArrayList<FileInfo> folders = Services.getFileSystemFolders().getFileSystemFolders();
        updateFilesystems(folders);
    }




    private void updateFilesystems(List<FileInfo> dirs) {

        mFilesystemScroll.setAdapter(new BaseQuickAdapter<FileInfo, BaseViewHolder>(R.layout.root_item_dir, dirs) {

            @Override
            protected void convert(@NonNull BaseViewHolder helper, FileInfo item) {
                final View view = helper.itemView;
                ImageView icon = view.findViewById(R.id.item_icon);
                TextView label = view.findViewById(R.id.item_name);
                if (item.getType() == FileInfo.TYPE_DOWNLOAD_DIR)
                    icon.setImageResource(R.drawable.folder_bookmark);
                else if (item.getType() == FileInfo.TYPE_FS_ROOT)
                    icon.setImageResource(R.drawable.media_flash_sd_mmc);
                else
                    icon.setImageResource(R.drawable.folder_blue);
                if (item.title != null)
                    label.setText(item.title); //  filename
                else if (item.getType() == FileInfo.TYPE_FS_ROOT || item.getType() == FileInfo.TYPE_DOWNLOAD_DIR)
                    label.setText(item.filename); //  filename
                else
                    label.setText(item.pathname); //  filename
                label.setMaxWidth(coverWidth * 25 / 10);
                view.setOnClickListener(v -> FileBrowserActivity.Companion.showDirectory(mActivity,item));
                view.setOnLongClickListener(view1 -> {
                    registerFoldersContextMenu(item);
                    return false;
                });
            }
        });

    }

    private void registerFoldersContextMenu(final FileInfo folder) {
        mActivity.registerForContextMenu(mFilesystemScroll);
        mFilesystemScroll.setOnCreateContextMenuListener((contextMenu, view, contextMenuInfo) -> {
            MenuInflater inflater = mActivity.getMenuInflater();
            inflater.inflate(R.menu.cr3_favorite_folder_context_menu, contextMenu);
            boolean isFavorite = folder.getType() == FileInfo.TYPE_NOT_SET;
            final FileSystemFolders service = Services.getFileSystemFolders();
            for (int idx = 0; idx < contextMenu.size(); ++idx) {
                MenuItem item = contextMenu.getItem(idx);
                boolean enabled = isFavorite;
                if (item.getItemId() == R.id.folder_left) {
                    enabled = enabled && service.canMove(folder, true);
                    if (enabled)
                        item.setOnMenuItemClickListener(menuItem -> {
                            service.moveFavoriteFolder(mActivity.getDB(), folder, true);
                            return true;
                        });
                } else if (item.getItemId() == R.id.folder_right) {
                    enabled = enabled && service.canMove(folder, false);
                    if (enabled)
                        item.setOnMenuItemClickListener(menuItem -> {
                            service.moveFavoriteFolder(mActivity.getDB(), folder, false);
                            return true;
                        });
                } else if (item.getItemId() == R.id.folder_remove) {
                    if (enabled)
                        item.setOnMenuItemClickListener(menuItem -> {
                            service.removeFavoriteFolder(mActivity.getDB(), folder);
                            return true;
                        });
                }
                item.setEnabled(enabled);
            }
        });
    }



    private void updateDelimiterTheme(int viewId) {
        View view = mView.findViewById(viewId);
        InterfaceTheme theme = mActivity.getCurrentTheme();
        view.setBackgroundResource(theme.getRootDelimiterResourceId());
        view.setMinimumHeight(theme.getRootDelimiterHeight());
        view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, theme.getRootDelimiterHeight()));
    }

    private void createViews() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View view = inflater.inflate(R.layout.root_window, null);
        mView = (ViewGroup) view;

        updateDelimiterTheme(R.id.delimiter1);
        updateDelimiterTheme(R.id.delimiter2);


        mRecentBooksScroll = mView.findViewById(R.id.scroll_recent_books);

        mFilesystemScroll = mView.findViewById(R.id.scroll_filesystem);



        updateCurrentBook(Services.getHistory().getLastBook());


        mView.findViewById(R.id.btn_menu).setOnClickListener(v -> showMenu());

        mView.findViewById(R.id.current_book).setOnClickListener(v -> {
            if (currentBook != null) {
                ReaderActivity.Companion.loadDocument(getContext(), currentBook.getFileInfo().pathname);
            }

        });
        mView.findViewById(R.id.current_book).setOnLongClickListener(v -> {
         /*   if (currentBook != null)
                mContext.editBookInfo(Services.getScanner().createRecentRoot(), currentBook.getFileInfo());*/
            return true;
        });

        refreshRecentBooks();

        Services.getFileSystemFolders().addListener((object, onlyProperties) -> BackgroundThread.instance().postGUI(new Runnable() {
            @Override
            public void run() {
                refreshFileSystemFolders();
            }
        }));

        mActivity.waitForCRDBService(() -> Services.getFileSystemFolders().loadFavoriteFolders(mActivity.getDB()));


        removeAllViews();
        addView(mView);
    }


    public void onCoverpagesReady(ArrayList<CoverpageManager.ImageItem> files) {
        //invalidate();
        log.d("CRRootView.onCoverpagesReady(" + files + ")");
        CoverpageManager.invalidateChildImages(mView, files);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        r -= l;
        b -= t;
        t = 0;
        l = 0;
        mView.layout(l, t, r, b);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mView.measure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mView.getMeasuredWidth(), mView.getMeasuredHeight());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void showMenu() {
        ReaderAction[] actions = {
                ReaderAction.ABOUT,
                ReaderAction.CURRENT_BOOK,
                ReaderAction.RECENT_BOOKS,
                ReaderAction.USER_MANUAL,
                ReaderAction.OPTIONS,
                ReaderAction.EXIT,
        };
        mActivity.showActionsPopupMenu(actions, item -> {
            if (item == ReaderAction.EXIT) {
                mActivity.finish();
                return true;
            } else if (item == ReaderAction.ABOUT) {
                mActivity.showAboutDialog();
                return true;
            } else if (item == ReaderAction.RECENT_BOOKS) {
                mActivity.showRecentBooks();
                return true;
            } else if (item == ReaderAction.CURRENT_BOOK) {
//				mContext.showCurrentBook();
                return true;
            } else if (item == ReaderAction.USER_MANUAL) {
//				mContext.showManual();
                return true;
            } else if (item == ReaderAction.OPTIONS) {
                mActivity.showBrowserOptionsDialog();
                return true;
            }
            return false;
        });
    }


}
