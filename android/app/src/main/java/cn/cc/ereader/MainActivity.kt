package cn.cc.ereader

import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Debug
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cn.cc.ereader.ui.HomeFragment
import cn.cyrus.translater.feater.HomeActivity
import org.coolreader.R
import org.coolreader.crengine.*
import org.coolreader.crengine.filebrowser.FileBrowserActivity
import org.koekak.android.ebookdownloader.SonyBookSelector

class MainActivity : BaseActivity() {
    companion object {
        val log = L.create("cr")

        internal val LOAD_LAST_DOCUMENT_ON_START = true

        fun dumpHeapAllocation() {
            val info = Debug.MemoryInfo()
            val fields = Debug.MemoryInfo::class.java.fields
            Debug.getMemoryInfo(info)
            val buf = StringBuilder()
            for (f in fields) {
                if (buf.length > 0)
                    buf.append(", ")
                buf.append(f.name)
                buf.append("=")
                buf.append(f.get(info))
            }
            log.d("nativeHeapAlloc=" + Debug.getNativeHeapAllocatedSize() + ", nativeHeapSize=" + Debug.getNativeHeapSize() + ", info: " + buf.toString())
        }

        val OPEN_FILE_PARAM = "FILE_TO_OPEN"


    }


    private var mHomeFrame: CRRootView? = null
    var mEngine: Engine? = null
    //View startupView;
    //CRDB mDB;
    private var mCurrentFrame: ViewGroup? = null
    var previousFrame: ViewGroup? = null
        private set
    internal var fileToLoadOnStart: String? = null
    private var isFirstStart = true
    private var justCreated = false
    internal var mDestroyed = false
    private var stopped = false
    val isPreviousFrameHome: Boolean
        get() = previousFrame != null && previousFrame === mHomeFrame
    private var mPreferences: SharedPreferences? = null
    private val prefs: SharedPreferences?
        get() {
            if (mPreferences == null)
                mPreferences = getSharedPreferences(PREF_FILE, 0)
            return mPreferences
        }

    internal var CURRENT_NOTIFICATOIN_VERSION = 1
    var lastNotificationId: Int
        get() {
            val res = prefs!!.getInt(PREF_LAST_NOTIFICATION, 0)
            log.i("getLastNotification() = $res")
            return res
        }
        set(notificationId) {
            try {
                val editor = prefs!!.edit()
                editor.putInt(PREF_LAST_NOTIFICATION, notificationId)
                editor.commit()
            } catch (e: Exception) {
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        startServices()
        super.onCreate(savedInstanceState)
        contentView = layoutInflater.inflate(R.layout.container, null)
        setContentView(contentView)
        // apply settings
        onSettingsChanged(settings(), null)

        isFirstStart = true
        justCreated = true

        mEngine = Engine.getInstance(this)

        volumeControlStream = AudioManager.STREAM_MUSIC
        N2EpdController.n2MainActivity = this

        showRootWindow()

        log.i("CoolReaderActivity.onCreate() exiting")

    }


    override fun onDestroy() {

        log.i("CoolReaderActivity.onDestroy() entered")




        if (mHomeFrame != null)
            mHomeFrame!!.onClose()
        mDestroyed = true


        log.i("CoolReaderActivity.onDestroy() exiting")
        super.onDestroy()

        Services.stopServices()
    }

    override fun applyAppSetting(key: String, value: String) {
        super.applyAppSetting(key, value)
     /*   val flg = "1" == value
        if (key == Settings.PROP_APP_KEY_BACKLIGHT_OFF) {
            isKeyBacklightDisabled = flg
        } else if (key == Settings.PROP_APP_SCREEN_BACKLIGHT_LOCK) {
            var n = 0
            try {
                n = Integer.parseInt(value)
            } catch (e: NumberFormatException) {
                // ignore
            }

            setScreenBacklightDuration(n)
        } else if (key == Settings.PROP_APP_BOOK_SORT_ORDER) {
            if (mBrowser != null)
                mBrowser!!.setSortOrder(value)
        } else if (key == Settings.PROP_APP_FILE_BROWSER_SIMPLE_MODE) {
            if (mBrowser != null)
                mBrowser!!.isSimpleViewMode = flg
        } else if (key == Settings.PROP_APP_SHOW_COVERPAGES) {
            if (mBrowser != null)
                mBrowser!!.setCoverPagesEnabled(flg)
        } else if (key == Settings.PROP_APP_BOOK_PROPERTY_SCAN_ENABLED) {
            Services.getScanner().dirScanEnabled = flg
        } else if (key == Settings.PROP_FONT_FACE) {
            if (mBrowser != null)
                mBrowser!!.setCoverPageFontFace(value)
        } else if (key == Settings.PROP_APP_COVERPAGE_SIZE) {
            var n = 0
            try {
                n = Integer.parseInt(value)
            } catch (e: NumberFormatException) {
                // ignore
            }

            if (n < 0)
                n = 0
            else if (n > 2)
                n = 2
            if (mBrowser != null)
                mBrowser!!.setCoverPageSizeOption(n)
        } else if (key == Settings.PROP_APP_FILE_BROWSER_SIMPLE_MODE) {
            if (mBrowser != null)
                mBrowser!!.isSimpleViewMode = flg
        } else if (key == Settings.PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS) {
            Services.getScanner().setHideEmptyDirs(flg)
        }*/
        //
    }


    private fun extractFileName(uri: Uri?): String? {
        return if (uri != null) {
            if (uri == Uri.parse("file:///"))
                null
            else
                uri.path
        } else null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        log.i("onNewIntent : $intent")
        if (mDestroyed) {
            log.e("engine is already destroyed")
            return
        }
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?): Boolean {
        log.d("intent=" + intent!!)
        if (intent == null)
            return false
        var fileToOpen: String? = null
        if (Intent.ACTION_VIEW == intent.action) {
            val uri = intent.data
            intent.data = null
            if (uri != null) {
                fileToOpen = uri.path
                //				if (fileToOpen.startsWith("file://"))
                //					fileToOpen = fileToOpen.substring("file://".length());
            }
        }
        if (fileToOpen == null && intent.extras != null) {
            log.d("extras=" + intent.extras!!)
            fileToOpen = intent.extras!!.getString(OPEN_FILE_PARAM)
        }
        showRootWindow()
        return true
        /*  if (fileToOpen != null) {
              // patch for opening of books from ReLaunch (under Nook Simple Touch)
              while (fileToOpen!!.indexOf("%2F") >= 0) {
                  fileToOpen = fileToOpen.replace("%2F", "/")
              }
              log.d("FILE_TO_OPEN = $fileToOpen")
              loadDocument(fileToOpen, Runnable {
                  showToast("Cannot open book")

              })
              return true
          } else {
              log.d("No file to open")
              return false
          }*/

    }

    override fun onPause() {
        super.onPause()
        Services.getCoverpageManager().removeCoverpageReadyListener(mHomeFrame)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        log.i("CoolReaderActivity.onPostCreate()")
        super.onPostCreate(savedInstanceState)
    }

    override fun onPostResume() {
        log.i("CoolReaderActivity.onPostResume()")
        super.onPostResume()
    }

    //	private boolean restarted = false;
    override fun onRestart() {
        log.i("CoolReaderActivity.onRestart()")
        //restarted = true;
        super.onRestart()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        log.i("CoolReaderActivity.onRestoreInstanceState()")
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onResume() {
        log.i("CoolReaderActivity.onResume()")
        super.onResume()
        //Properties props = SettingsManager.instance(this).get();


        if (DeviceInfo.EINK_SCREEN) {
            if (DeviceInfo.EINK_SONY) {
                val pref = getSharedPreferences(BaseActivity.PREF_FILE, 0)
                val res = pref.getString(PREF_LAST_BOOK, null)
                if (res != null && res.length > 0) {
                    val selector = SonyBookSelector(this)
                    val l = selector.getContentId(res)
                    if (l != 0L) {
                        selector.setReadingTime(l)
                        selector.requestBookSelection(l)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        log.i("CoolReaderActivity.onSaveInstanceState()")
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        log.i("CoolReaderActivity.onStart() version=$version, fileToLoadOnStart=$fileToLoadOnStart")
        super.onStart()

        if (mHomeFrame == null) {
            waitForCRDBService {
                Services.getHistory().loadFromDB(db!!, 200)

                val m = HomeFragment(this)
                mHomeFrame = m.mHomeFrame

                Services.getCoverpageManager().addCoverpageReadyListener(mHomeFrame)
                mHomeFrame!!.requestFocus()

                showRootWindow()
                setSystemUiVisibility()

                notifySettingsChanged()

                showNotifications()
            }
        }



        if (!isFirstStart)
            return
        isFirstStart = false

        if (justCreated) {
            justCreated = false
            processIntent(intent)
        }
        stopped = false

        log.i("CoolReaderActivity.onStart() exiting")
    }

    override fun onStop() {
        log.i("CoolReaderActivity.onStop() entering")
        // Donations support code
        super.onStop()
        stopped = true
        // will close book at onDestroy()


        log.i("CoolReaderActivity.onStop() exiting")
    }


    override fun setCurrentTheme(theme: InterfaceTheme) {
        super.setCurrentTheme(theme)
        if (mHomeFrame != null)
            mHomeFrame!!.onThemeChange(theme)
        //getWindow().setBackgroundDrawable(theme.getActionBarBackgroundDrawableBrowser());
    }


    override fun onSettingsChanged(props: Properties, oldProps: Properties?) {
        val changedProps = if (oldProps != null) props.diff(oldProps) else props
        if (mHomeFrame != null) {
            mHomeFrame!!.refreshOnlineCatalogs()
        }

        for ((key1, value1) in changedProps) {
            val key = key1 as String
            val value = value1 as String
            applyAppSetting(key, value)
        }

    }


    private fun setCurrentFrame(newFrame: ViewGroup?) {
        if (mCurrentFrame !== newFrame) {
            previousFrame = mCurrentFrame
            log.i("New current frame: " + newFrame!!.javaClass.toString())
            mCurrentFrame = newFrame


            supportFragmentManager.inTransaction {
                replace(R.id.container, mCurrentFrame!!.tag as Fragment)
            }

            mCurrentFrame!!.requestFocus()
            if (mCurrentFrame === mHomeFrame) {
                // update recent books
                mHomeFrame!!.refreshRecentBooks()
//                setLastLocationRoot()
                mCurrentFrame!!.invalidate()
            }

            onUserActivity()
        }
    }


    fun showRootWindow() {
        setCurrentFrame(mHomeFrame)
    }

    fun showAboutDialog() {
        val dlg = AboutDialog(this@MainActivity)
        dlg.show()
    }

    fun showNotifications() {
        val lastNoticeId = lastNotificationId
        if (lastNoticeId >= CURRENT_NOTIFICATOIN_VERSION)
            return
        if (DeviceInfo.getSDKLevel() >= DeviceInfo.HONEYCOMB)
            if (lastNoticeId <= 1)
                notification1()
        lastNotificationId = CURRENT_NOTIFICATOIN_VERSION
    }

    fun notification1() {
        if (hasHardwareMenuKey())
            return  // don't show notice if hard key present
        showNotice(R.string.note1_reader_menu, { setSetting(Settings.PROP_TOOLBAR_LOCATION, Settings.VIEWER_TOOLBAR_SHORT_SIDE.toString(), false) }, { setSetting(Settings.PROP_TOOLBAR_LOCATION, Settings.VIEWER_TOOLBAR_NONE.toString(), false) })
    }

    /**
     * Open location - book, root view, folder...
     */
    /* fun showLastLocation() {
         var location = lastLocation
         if (location == null)
             location = FileInfo.ROOT_DIR_TAG
         if (location.startsWith(BOOK_LOCATION_PREFIX)) {
             location = location.substring(BOOK_LOCATION_PREFIX.length)
             loadDocument(location, null)

             return
         }
         if (location.startsWith(DIRECTORY_LOCATION_PREFIX)) {
             location = location.substring(DIRECTORY_LOCATION_PREFIX.length)
             showBrowser(location)
             return
         }
         if (location == FileInfo.RECENT_DIR_TAG) {
             showBrowser(location)
             return
         }
         // TODO: support other locations as well
         showRootWindow()
     }*/

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings ->
                startActivity(Intent(this, HomeActivity::class.java))
        }
        return true
    }

    fun showBrowser(dir: String) {
        FileBrowserActivity.showDirectory(this,Services.getScanner().pathToFileInfo(dir))
    }

    fun showRecentBooks() {
        FileBrowserActivity.showDirectory(this,Services.getScanner().getRecentDir())
    }



}

