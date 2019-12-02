package cn.cc.ereader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cn.cc.ereader.MainActivity
import org.coolreader.R
import org.coolreader.crengine.*

class FileBroswerFragment(activity: MainActivity) : Fragment() {

    var mBrowser: FileBrowser? = null
    var mBrowserTitleBar: View? = null
    var mBrowserToolBar: CRToolBar? = null
    var mBrowserFrame: BrowserViewLayout? = null


    init {

        mBrowser = FileBrowser(activity, Services.getEngine(), Services.getScanner(), Services.getHistory())
        mBrowser!!.setCoverPagesEnabled(activity.settings().getBool(ReaderView.PROP_APP_SHOW_COVERPAGES, true))
        mBrowser!!.setCoverPageFontFace(activity.settings().getProperty(ReaderView.PROP_FONT_FACE, DeviceInfo.DEF_FONT_FACE))
        mBrowser!!.setCoverPageSizeOption(activity.settings().getInt(ReaderView.PROP_APP_COVERPAGE_SIZE, 1))
        mBrowser!!.setSortOrder(activity.settings().getProperty(ReaderView.PROP_APP_BOOK_SORT_ORDER))
        mBrowser!!.isSimpleViewMode = activity.settings().getBool(ReaderView.PROP_APP_FILE_BROWSER_SIMPLE_MODE, false)
        mBrowser!!.init()


        val inflater = LayoutInflater.from(activity)// activity.getLayoutInflater();

        mBrowserTitleBar = inflater.inflate(R.layout.browser_status_bar, null)
        activity.setBrowserTitle("Cool Reader browser window")


        mBrowserToolBar = CRToolBar(activity, ReaderAction.createList(
                ReaderAction.FILE_BROWSER_UP,
                ReaderAction.CURRENT_BOOK,
                ReaderAction.OPTIONS,
                ReaderAction.FILE_BROWSER_ROOT,
                ReaderAction.RECENT_BOOKS,
                ReaderAction.CURRENT_BOOK_DIRECTORY,
                ReaderAction.OPDS_CATALOGS,
                ReaderAction.SEARCH,
                ReaderAction.SCAN_DIRECTORY_RECURSIVE,
                ReaderAction.EXIT
        ), false)
        mBrowserToolBar!!.setBackgroundResource(R.drawable.ui_status_background_browser_dark)
        mBrowserToolBar!!.setOnActionHandler { item ->
            when (item.cmd) {
                ReaderCommand.DCMD_EXIT ->
                    //
                    activity.finish()
                ReaderCommand.DCMD_FILE_BROWSER_ROOT -> activity.showRootWindow()
                ReaderCommand.DCMD_FILE_BROWSER_UP -> mBrowser!!.showParentDirectory()
                ReaderCommand.DCMD_OPDS_CATALOGS -> mBrowser!!.showOPDSRootDirectory()
                ReaderCommand.DCMD_RECENT_BOOKS_LIST -> mBrowser!!.showRecentBooks()
                ReaderCommand.DCMD_SEARCH -> mBrowser!!.showFindBookDialog()
                ReaderCommand.DCMD_CURRENT_BOOK -> activity.showCurrentBook()
                ReaderCommand.DCMD_OPTIONS_DIALOG -> activity.showBrowserOptionsDialog()
                ReaderCommand.DCMD_SCAN_DIRECTORY_RECURSIVE -> mBrowser!!.scanCurrentDirectoryRecursive()
                else -> {
                }
            }// do nothing
            false
        }

        mBrowserFrame = BrowserViewLayout(activity, mBrowser, mBrowserToolBar, mBrowserTitleBar)
        mBrowserFrame!!.tag = this
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return mBrowserFrame
    }
}