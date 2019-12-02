package cn.cc.ereader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cn.cc.ereader.MainActivity
import org.coolreader.crengine.CRRootView


class HomeFragment(activity: MainActivity) : Fragment()  {

    var mHomeFrame:CRRootView?= null
    init {
        mHomeFrame = CRRootView(activity)
        mHomeFrame!!.tag = this
    }




    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return mHomeFrame
    }

}