package org.coolreader.crengine.filebrowser

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import cn.cc.ereader.MainActivity
import org.coolreader.R
import org.coolreader.crengine.*

class FileBrowserActivity : BaseActivity() {

    val OPEN_DIR_PARAM = "DIR_TO_OPEN"
    private val BOOK_LOCATION_PREFIX = "@book:"
    private val DIRECTORY_LOCATION_PREFIX = "@dir:"
    var mBrowser: FileBrowser? = null
    var mBrowserTitleBar: View? = null
    var mBrowserToolBar: CRToolBar? = null
    var mBrowserFrame: BrowserViewLayout? = null
    private var mPreferences: SharedPreferences? = null
    private val prefs: SharedPreferences?
        get() {
            if (mPreferences == null)
                mPreferences = getSharedPreferences(PREF_FILE, 0)
            return mPreferences
        }
    val isBrowserCreated: Boolean
        get() = mBrowserFrame != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        waitForCRDBService {
            mBrowser = FileBrowser(this, Services.getEngine(), Services.getScanner(), Services.getHistory())
            mBrowser!!.setCoverPagesEnabled(this.settings().getBool(ReaderView.PROP_APP_SHOW_COVERPAGES, true))
            mBrowser!!.setCoverPageFontFace(this.settings().getProperty(ReaderView.PROP_FONT_FACE, DeviceInfo.DEF_FONT_FACE))
            mBrowser!!.setCoverPageSizeOption(this.settings().getInt(ReaderView.PROP_APP_COVERPAGE_SIZE, 1))
            mBrowser!!.setSortOrder(this.settings().getProperty(ReaderView.PROP_APP_BOOK_SORT_ORDER))
            mBrowser!!.isSimpleViewMode = this.settings().getBool(ReaderView.PROP_APP_FILE_BROWSER_SIMPLE_MODE, false)
            mBrowser!!.init()


            val inflater = LayoutInflater.from(this)// activity.getLayoutInflater();

            mBrowserTitleBar = inflater.inflate(R.layout.browser_status_bar, null)
            this.setBrowserTitle("Cool Reader browser window")


            mBrowserToolBar = CRToolBar(this, ReaderAction.createList(
                    ReaderAction.FILE_BROWSER_UP,
                    ReaderAction.OPTIONS,
                    ReaderAction.FILE_BROWSER_ROOT
            ), false)
            mBrowserToolBar!!.setBackgroundResource(R.drawable.ui_status_background_browser_dark)
            mBrowserToolBar!!.setOnActionHandler { item ->
                when (item.cmd) {
                    ReaderCommand.DCMD_EXIT ->finish()
                    ReaderCommand.DCMD_FILE_BROWSER_ROOT -> mBrowser!!.showDirectory(Services.getScanner().libraryItems[0],null)
                    ReaderCommand.DCMD_FILE_BROWSER_UP -> mBrowser!!.showParentDirectory()
                    ReaderCommand.DCMD_OPDS_CATALOGS -> mBrowser!!.showOPDSRootDirectory()
                    ReaderCommand.DCMD_RECENT_BOOKS_LIST -> mBrowser!!.showDirectory(Services.getScanner().getRecentDir(), null)
                    ReaderCommand.DCMD_SEARCH -> mBrowser!!.showFindBookDialog()
                    ReaderCommand.DCMD_OPTIONS_DIALOG -> showBrowserOptionsDialog()
                    ReaderCommand.DCMD_SCAN_DIRECTORY_RECURSIVE -> mBrowser!!.scanCurrentDirectoryRecursive()
                    else -> {
                    }
                }// do nothing
                false
            }

            mBrowserFrame = BrowserViewLayout(this, mBrowser, mBrowserToolBar, mBrowserTitleBar)
            mBrowserFrame!!.tag = this
            setContentView(mBrowserFrame)
            showDirectory(intent.getSerializableExtra(KEY_FILE_INFO) as FileInfo)
        }
    }

    fun askDeleteBook(item: FileInfo) {
        askConfirmation(R.string.win_title_confirm_book_delete) {
            //            closeBookIfOpened(item)
            var file = Services.getScanner().findFileInTree(item)
            if (file == null)
                file = item
            if (file.deleteFile()) {
                Services.getHistory().removeBookInfo(db, file, true, true)
            }
            if (file.parent != null)
                directoryUpdated(file.parent)
        }
    }

    fun askDeleteRecent(item: FileInfo) {
        askConfirmation(R.string.win_title_confirm_history_record_delete) {
            Services.getHistory().removeBookInfo(db, item, true, false)
            directoryUpdated(Services.getScanner().createRecentRoot())
        }
    }

    fun askDeleteCatalog(item: FileInfo?) {
        askConfirmation(R.string.win_title_confirm_catalog_delete) {
            if (item != null && item.isOPDSDir) {
                db!!.removeOPDSCatalog(item.id)
                directoryUpdated(Services.getScanner().createRecentRoot())
            }
        }
    }


    fun editBookInfo(currDirectory: FileInfo, item: FileInfo) {
        Services.getHistory().getOrCreateBookInfo(db, item) { bookInfo1 ->
            var bookInfo = bookInfo1
            if (bookInfo == null)
                bookInfo = BookInfo(item)
            val dlg = BookInfoEditDialog(this, currDirectory, bookInfo,
                    currDirectory.isRecentDir)
            dlg.show()
        }
    }

    fun directoryUpdated(dir: FileInfo, selected: FileInfo?) {

        /*  if (dir.isOPDSRoot)
              mHomeFrame!!.refreshOnlineCatalogs()
          else if (dir.isRecentDir)
              mHomeFrame!!.refreshRecentBooks()*/
        if (mBrowser != null)
            mBrowser!!.refreshDirectory(dir, selected)
    }


    override fun directoryUpdated(dir: FileInfo) {
        directoryUpdated(dir, null)
    }

    fun editOPDSCatalog(opds: FileInfo?) {
        var opds = opds
        if (opds == null) {
            opds = FileInfo()
            opds.isDirectory = true
            opds.pathname = FileInfo.OPDS_DIR_PREFIX + "http://"
            opds.filename = "New Catalog"
            opds.isListed = true
            opds.isScanned = true
            opds.parent = Services.getScanner().opdsRoot
        }
        val dlg = OPDSCatalogEditDialog(this, opds, Runnable { refreshOPDSRootDirectory(true) })
        dlg.show()
    }

    fun refreshOPDSRootDirectory(showInBrowser: Boolean) {
        if (mBrowser != null)
            mBrowser!!.refreshOPDSRootDirectory(showInBrowser)
        /* if (mHomeFrame != null)
             mHomeFrame!!.refreshOnlineCatalogs()*/
    }

    fun setBrowserTitle(title: String) {
        if (mBrowserFrame != null)
            mBrowserFrame!!.setBrowserTitle(title)
    }

    var lastLocation: String?
        get() {
            var res = prefs!!.getString(PREF_LAST_LOCATION, null)
            if (res == null) {
                res = prefs!!.getString(PREF_LAST_BOOK, null)
                if (res != null) {
                    res = BOOK_LOCATION_PREFIX + res
                    try {
                        prefs!!.edit().remove(PREF_LAST_BOOK).commit()
                    } catch (e: Exception) {
                    }

                }
            }
            return res
        }
        set(location) {
            try {
                val oldLocation = prefs!!.getString(PREF_LAST_LOCATION, null)
                if (oldLocation != null && oldLocation == location)
                    return
                val editor = prefs!!.edit()
                editor.putString(PREF_LAST_LOCATION, location)
                editor.commit()
            } catch (e: Exception) {
            }

        }


    fun setLastDirectory(path: String) {
        lastLocation = DIRECTORY_LOCATION_PREFIX + path
    }

    fun setLastLocationRoot() {
        lastLocation = FileInfo.ROOT_DIR_TAG
    }

    private fun runInBrowser(task: Runnable) {
        waitForCRDBService {
            task.run()
        }

    }


    companion object {
        const val KEY_FILE_INFO = "key_file_info"
        fun showDirectory(ctx: Context, dir: FileInfo) {
            val intent = Intent(ctx, FileBrowserActivity::class.java)
            intent.putExtra(KEY_FILE_INFO, dir)
            ctx.startActivity(intent)
        }
    }

    fun showBrowser(dir: FileInfo) {
        runInBrowser(Runnable { mBrowser!!.showDirectory(dir, null) })
    }


    fun showDirectory(path: FileInfo) {
        MainActivity.log.d("Activities.showDirectory($path) is called")
        showBrowser(path)
    }

    fun showOnlineCatalogs() {
        MainActivity.log.d("Activities.showOnlineCatalogs() is called")
        runInBrowser(Runnable { mBrowser!!.showOPDSRootDirectory() })
    }

}