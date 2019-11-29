package cn.cc.ereader.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import org.coolreader.R
import org.coolreader.crengine.BookInfo

class HomeFragment : Fragment() {

//    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
     /*   homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel::class.java)*/
        val root = inflater.inflate(R.layout.root_window, container, false)

        val rcBooks = root.findViewById<RecyclerView>(R.id.rc_recent_books)
        rcBooks.layoutManager = LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false)
        rcBooks.adapter = object :BaseQuickAdapter<BookInfo,BaseViewHolder>(R.layout.root_item_recent_book){
            override fun convert(helper: BaseViewHolder, item: BookInfo?) {

            }

        }
//        val textView: TextView = root.findViewById(R.id.text_home)
      /*  homeViewModel.text.observe(this, Observer {
            textView.text = it
        })*/
        return root
    }
}