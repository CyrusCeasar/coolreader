package org.coolreader.crengine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import cn.cc.ereader.MainActivity
import cn.cc.ereader.TranslateResultActivity
import org.coolreader.PhoneStateReceiver

class ReaderActivity : BaseActivity() {

    // ========================================================================================
    // TTS
    internal var tts: TTS? = null
    internal var ttsInitialized: Boolean = false
    internal var ttsError: Boolean = false


    var readerView: ReaderView? = null
        private set
    var mReaderFrame: ReaderViewLayout? = null
    // ============================================================
    private var am: AudioManager? = null
    private var maxVolume: Int = 0
    val isBookOpened: Boolean
        get() = if (readerView == null) false else readerView!!.isBookLoaded

    val audioManager: AudioManager?
        get() {
            if (am == null) {
                am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                maxVolume = am!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            }
            return am
        }

    var volume: Int
        get() {
            val am = audioManager
            return if (am != null) {
                am.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / maxVolume
            } else 0
        }
        set(volume) {
            val am = audioManager
            am?.setStreamVolume(AudioManager.STREAM_MUSIC, volume * maxVolume / 100, 0)
        }
    internal var initialBatteryState = -1
    internal var intentReceiver: BroadcastReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // apply settings
        onSettingsChanged(settings(), null)

        readerView = ReaderView(this, Engine.getInstance(this), this.settings())
        mReaderFrame = ReaderViewLayout(this, readerView!!)
        setContentView(mReaderFrame)
        mReaderFrame!!.toolBar.setOnActionHandler { item ->
            if (readerView != null)
                readerView!!.onAction(item)
            true
        }
        mReaderFrame!!.tag = this
        loadDocument(intent.getStringExtra(KEY_FILEINFO)!!)


        // Battery state listener
        intentReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra("level", 0)
                if (readerView != null)
                    readerView!!.batteryState = level
                else
                    initialBatteryState = level
            }

        }
        registerReceiver(intentReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        if (initialBatteryState >= 0 && readerView != null)
            readerView!!.batteryState = initialBatteryState
    }

    // Dictionary support

    fun findInDictionary(s: String?) {

        if (s != null && s.length != 0) {
            var start: Int
            var end: Int

            // Skip over non-letter characters at the beginning and end of the search string
            start = 0
            while (start < s.length) {
                if (Character.isLetterOrDigit(s[start]))
                    break
                start++
            }
            end = s.length - 1
            while (end >= start) {
                if (Character.isLetterOrDigit(s[end]))
                    break
                end--
            }

            if (end > start) {
                val pattern = s.substring(start, end + 1)
                TranslateResultActivity.startThis(pattern, this)
            }
        }
    }

    override fun onDestroy() {

        if (tts != null) {
            tts!!.shutdown()
            tts = null
            ttsInitialized = false
            ttsError = false
        }
        if (readerView != null)
            readerView!!.close()
        if (intentReceiver != null) {
            unregisterReceiver(intentReceiver)
            intentReceiver = null
        }


        if (readerView != null) {
            readerView!!.destroy()
        }
        readerView = null
        super.onDestroy()

    }

    override fun onPause() {
        super.onPause()
        if (readerView != null)
            readerView!!.onAppPause()
    }
    /* override fun setDimmingAlpha(dimmingAlpha: Int) {
         if (readerView != null)
             readerView!!.setDimmingAlpha(dimmingAlpha)
     }*/

    fun saveSetting(name: String, value: String) {
        if (readerView != null)
            readerView!!.saveSetting(name, value)
    }


    fun closeBookIfOpened(book: FileInfo) {
        if (readerView == null)
            return
        readerView!!.closeIfOpened(book)
    }

    fun showOptionsDialog(mode: OptionsDialog.Mode) {
        BackgroundThread.instance().postBackground {
            val mFontFaces = Engine.getFontFaceList()
            BackgroundThread.instance().executeGUI {
                val dlg = OptionsDialog(this, readerView, mFontFaces, mode)
                dlg.show()
            }
        }
    }

    override fun onSettingsChanged(props: Properties?, oldProps: Properties?) {
        super.onSettingsChanged(props, oldProps)
        if (mReaderFrame != null) {
            mReaderFrame!!.updateSettings(props)
            if (readerView != null)
                readerView!!.updateSettings(props)
        }
    }

    override fun onStart() {
        super.onStart()
        PhoneStateReceiver.setPhoneActivityHandler {
            if (readerView != null) {
                readerView!!.stopTTS()
                readerView!!.save()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (readerView != null)
            readerView!!.onAppResume()
    }

    fun initTTS(listener: TTS.OnTTSCreatedListener): Boolean {
        if (ttsError || !TTS.isFound()) {
            if (!ttsError) {
                ttsError = true
                showToast("TTS is not available")
            }
            return false
        }
        if (ttsInitialized && tts != null) {
            BackgroundThread.instance().executeGUI { listener.onCreated(tts) }
            return true
        }
        if (ttsInitialized && tts != null) {
            showToast("TTS initialization is already called")
            return false
        }
        showToast("Initializing TTS")
        tts = TTS(this, TTS.OnInitListener { status ->
            //tts.shutdown();
            L.i("TTS init status: $status")
            if (status == TTS.SUCCESS) {
                ttsInitialized = true
                BackgroundThread.instance().executeGUI { listener.onCreated(tts) }
            } else {
                ttsError = true
                BackgroundThread.instance().executeGUI { showToast("Cannot initialize TTS") }
            }
        })
        return true
    }

    fun showBookmarksDialog() {
        BackgroundThread.instance().executeGUI {
            val dlg = BookmarksDlg(this, readerView)
            dlg.show()
        }
    }

    fun openURL(url: String) {
        try {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        } catch (e: Exception) {
            MainActivity.log.e("Exception $e while trying to open URL $url")
            showToast("Cannot open URL $url")
        }

    }


    fun sendBookFragment(bookInfo: BookInfo, text: String) {
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "text/plain"
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, bookInfo.fileInfo.getAuthors() + " " + bookInfo.fileInfo.getTitle())
        emailIntent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(emailIntent, null))
    }

    fun showManual() {
        loadDocument("@manual", null)
    }

    fun loadDocument(item: String, callback: Runnable? = null) {
        runInReader(Runnable { readerView!!.loadDocument(item, callback) })
    }

    private fun runInReader(task: Runnable) {
        waitForCRDBService {
            if (mReaderFrame != null) {
                task.run()
                if (readerView != null && readerView!!.surface != null) {
                    readerView!!.surface.isFocusable = true
                    readerView!!.surface.isFocusableInTouchMode = true
                    readerView!!.surface.requestFocus()
                } else {
                    MainActivity.log.w("runInReader: mReaderView or mReaderView.getSurface() is null")
                }
            }
        }

    }

    fun loadDocument(item: FileInfo) {
        loadDocument(item.pathName)
    }

    fun loadDocument(item: FileInfo, callback: Runnable? = null) {
        MainActivity.log.d("Activities.loadDocument(" + item.pathname + ")")
        loadDocument(item.pathName, callback)
    }

    companion object {
        const val KEY_FILEINFO = "key_file_info"
        fun loadDocument(ctx: Context, path: String) {
            val intent = Intent(ctx, ReaderActivity::class.java)
            intent.putExtra(KEY_FILEINFO, path)
            ctx.startActivity(intent)
        }

    }

    fun showReader() {

    }

    fun showReaderMenu() {
        //
        if (mReaderFrame != null) {
            mReaderFrame!!.showMenu()
        }
    }

    fun updateCurrentPositionStatus(book: FileInfo, position: Bookmark, props: PositionProperties) {
        mReaderFrame!!.statusBar.updateCurrentPositionStatus(book, position, props)
    }

}