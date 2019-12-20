package cn.cc.ereader

import android.view.LayoutInflater
import android.view.View
import cn.cyrus.translater.base.BaseLazyInitFragment
import org.coolreader.crengine.CRRootView
import org.coolreader.crengine.Services

class ReaderFragment: BaseLazyInitFragment() {
    var mHomeFrame: CRRootView? = null
    override fun initView(layoutInflater: LayoutInflater): View? {
        mHomeFrame =  CRRootView(activity as MainActivity?)
        return mHomeFrame
    }

    override fun onPause() {
        super.onPause()
        Services.getCoverpageManager().removeCoverpageReadyListener(mHomeFrame)
    }
    override fun onDestroy() {
        super.onDestroy()
        mHomeFrame!!.onClose()
    }
}