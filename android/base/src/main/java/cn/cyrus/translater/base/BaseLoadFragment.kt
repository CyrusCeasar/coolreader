package cn.cyrus.translater.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.cyrus.translater.R
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder


/**
 * Created by ChenLei on 2018/8/21 0021.
 */
abstract class BaseLoadFragment<T,F> : BaseLazyInitFragment() {

    val mDatas: ArrayList<T> = ArrayList()

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mSwipeRecyclerView: SwipeRefreshLayout
    lateinit var mBaseQuickAdapter: BaseQuickAdapter<T, BaseViewHolder>
    lateinit var mPageManager:PageManager<F>

    val onUpdateListener: (List<T>?) -> Unit = {
        load(it)
    }

    val onLoadMoreListener: (List<T>?) -> Unit = {
        loadMore(it)
    }

    fun loadMore(datas: List<T>?) {
        if (datas ==null || datas.isEmpty()) {
            mBaseQuickAdapter.loadMoreEnd()
        } else {
            mPageManager.toNextPage()
            mDatas.addAll(datas.asIterable())
            mBaseQuickAdapter.loadMoreComplete()
        }
    }
    fun loadMoreError(){
        mBaseQuickAdapter.loadMoreFail()
    }

    fun updateError(){
        mSwipeRecyclerView.isRefreshing= false
        mBaseQuickAdapter.notifyDataSetChanged()
    }


    fun load(datas: List<T>?) {
        mDatas.clear()
        datas?.let {
            mDatas.addAll(datas.asIterable())
        }
        mPageManager.toNextPage()
        mSwipeRecyclerView.isRefreshing = false
        mBaseQuickAdapter.notifyDataSetChanged()
    }

    fun injectView(recyclerView: RecyclerView, swipeRefreshLayout: SwipeRefreshLayout) {
        mRecyclerView = recyclerView
        mSwipeRecyclerView = swipeRefreshLayout
        mSwipeRecyclerView.setOnRefreshListener {
            load(onUpdateListener)
        }
        mRecyclerView.layoutManager= LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mBaseQuickAdapter = getAdapter()
        mRecyclerView.adapter = mBaseQuickAdapter
        mBaseQuickAdapter.setOnLoadMoreListener({
            loadMore(onLoadMoreListener)
        }, mRecyclerView)
        mBaseQuickAdapter.emptyView = getEmptyView()
    }

    override fun initView(layoutInflater: LayoutInflater): View? {
        val view:ViewGroup = layoutInflater.inflate(R.layout.layout_base_load, null) as ViewGroup
        injectView(view.findViewById(R.id.rc), view as SwipeRefreshLayout)
        onInitFinished = {
            load(onUpdateListener)
        }
        mPageManager = getPageManager()
        return view
    }


    abstract fun load(onUpdate: (List<T>?) -> Unit)
    abstract fun loadMore(onLoadMore: (List<T>?) -> Unit)
    abstract fun getAdapter(): BaseQuickAdapter<T, BaseViewHolder>
    abstract fun getPageManager(): PageManager<F>
    fun getEmptyView():View{
        return  getEmptyView("没有数据",layoutInflater)
    }



}