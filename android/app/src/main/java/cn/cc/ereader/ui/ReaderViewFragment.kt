package cn.cc.ereader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cn.cc.ereader.MainActivity
import org.coolreader.R
import org.coolreader.crengine.*

class ReaderViewFragment(activity:MainActivity): Fragment() {
    var readerView: ReaderView? = null
        private set
     var mReaderFrame: ReaderViewLayout? = null
    init{
        readerView = ReaderView(activity, activity.mEngine, activity.settings())
        mReaderFrame = ReaderViewLayout(activity, readerView!!)
        mReaderFrame!!.toolBar.setOnActionHandler { item ->
            if (readerView != null)
                readerView!!.onAction(item)
            true
        }
        mReaderFrame!!.tag = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return  mReaderFrame
    }
}