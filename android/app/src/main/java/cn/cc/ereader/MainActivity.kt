package cn.cc.ereader

import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.os.Debug
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import cn.cyrus.translater.feater.RecordListFragment
import cn.cyrus.translater.feater.TranslateFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.app_bar_main.*
import org.coolreader.R
import org.coolreader.crengine.*
import org.coolreader.crengine.filebrowser.FileBrowserActivity
import org.koekak.android.ebookdownloader.SonyBookSelector

class MainActivity : BaseActivity() {
    val log = L.create("cr")!!

    companion object {
        fun dumpHeapAllocation() {
            val info = Debug.MemoryInfo()
            val fields = Debug.MemoryInfo::class.java.fields
            Debug.getMemoryInfo(info)
            val buf = StringBuilder()
            for (f in fields) {
                if (buf.isNotEmpty())
                    buf.append(", ")
                buf.append(f.name)
                buf.append("=")
                buf.append(f.get(info))
            }
        }
    }


    var mEngine: Engine? = null

    private var fileToLoadOnStart: String? = null
    private var isFirstStart = true
    private var justCreated = false
    private var mDestroyed = false
    private var stopped = false

    private var mPreferences: SharedPreferences? = null
    private val prefs: SharedPreferences?
        get() {
            if (mPreferences == null)
                mPreferences = getSharedPreferences(PREF_FILE, 0)
            return mPreferences
        }

    private var CURRENT_NOTIFICATOIN_VERSION = 1
    var lastNotificationId: Int
        get() {
            val res = prefs!!.getInt(PREF_LAST_NOTIFICATION, 0)
            log.i("getLastNotification() = $res")
            return res
        }
        set(notificationId) {
            val editor = prefs!!.edit()
            editor.putInt(PREF_LAST_NOTIFICATION, notificationId)
            editor.apply()
        }
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    val fragments = ArrayList<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startServices()
        // apply settings
        onSettingsChanged(mSettingsManager.mSettings, null)

        isFirstStart = true
        justCreated = true

        mEngine = Engine.getInstance(this)

        volumeControlStream = AudioManager.STREAM_MUSIC
        N2EpdController.n2MainActivity = this

        contentView = layoutInflater.inflate(R.layout.activity_translater,null)
        setContentView(contentView)
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        findViewById<ViewPager>(cn.cyrus.translater.R.id.container).adapter = mSectionsPagerAdapter

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }



        fragments.add(ReaderFragment())
        fragments.add(TranslateFragment())
        fragments.add(RecordListFragment.newInstance(RecordListFragment.TYPE_QUERY_NUM_ASC))
        fragments.add(RecordListFragment.newInstance(RecordListFragment.TYPE_TIME_DESC))
        mSectionsPagerAdapter!!.notifyDataSetChanged()

    }


    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a RecordListFragment (defined as a static inner class below).
            return fragments[position]
        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return fragments.size
        }
    }


    override fun onDestroy() {

        mDestroyed = true
        super.onDestroy()
        Services.stopServices()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        log.i("onNewIntent : $intent")
        if (mDestroyed) {
            log.e("engine is already destroyed")
            return
        }
        onUserActivity()
    }


    override fun onResume() {
        log.i("CoolReaderActivity.onResume()")
        super.onResume()

        if (DeviceInfo.EINK_SCREEN) {
            if (DeviceInfo.EINK_SONY) {
                val pref = getSharedPreferences(PREF_FILE, 0)
                val res = pref.getString(PREF_LAST_BOOK, null)
                if (res != null && res.isNotEmpty()) {
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


    override fun onStart() {
        log.i("CoolReaderActivity.onStart() version=$version, fileToLoadOnStart=$fileToLoadOnStart")
        super.onStart()

        waitForCRDBService {
            Services.getHistory().loadFromDB(db!!, 200)

            onUserActivity()
            setSystemUiVisibility()

            notifySettingsChanged()
            showNotifications()

        }

        if (!isFirstStart)
            return
        isFirstStart = false

        if (justCreated) {
            justCreated = false
            onUserActivity()
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


    override fun onSettingsChanged(props: Properties, oldProps: Properties?) {
        val changedProps = if (oldProps != null) props.diff(oldProps) else props

        for ((key1, value1) in changedProps) {
            val key = key1 as String
            val value = value1 as String
            applyAppSetting(key, value)
        }

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
//            R.id.action_settings ->
//                startActivity(Intent(this, HomeActivity::class.java))
        }
        return true
    }

    fun showBrowser(dir: String) {
        FileBrowserActivity.showDirectory(this, Services.getScanner().pathToFileInfo(dir))
    }

    fun showRecentBooks() {
        FileBrowserActivity.showDirectory(this, Services.getScanner().getRecentDir())
    }


}

