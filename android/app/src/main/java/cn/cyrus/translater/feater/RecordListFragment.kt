package cn.cyrus.translater.feater

import android.graphics.Canvas
import android.os.Bundle
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import cn.cyrus.translater.base.*
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.listener.OnItemSwipeListener
import io.reactivex.functions.Consumer
import org.coolreader.R

/**
 * Created by ChenLei on 2018/8/21 0021.
 */
/**
 * A placeholder fragment containing a simple view.
 */
class RecordListFragment : DefaultLoadMoreFragment<TranslateRecord>() {
    override fun load(onUpdate: (List<TranslateRecord>) -> Unit) {
        mPageManager.reset()
        syncWrok(serv.recordList(arguments!!.getInt(ARG_SECTION_TYPE), mPageManager.pageIndicator!!), Consumer {
            if (it.isResultOk()) {
                onUpdateListener.invoke(it.data!!)
            } else {
                updateError()
            }
        })
    }

    private val serv: TranslateRecordService = RetrofitManager.instance.create(TranslateRecordService::class.java)
    override fun loadMore(onLoadMore: (List<TranslateRecord>) -> Unit) {
        syncWrok(serv.recordList(1, mPageManager.pageIndicator!!), Consumer {
            if (it.isResultOk()) {
                onLoadMore.invoke(it.data!!)
            } else {
                loadMoreError()
            }
        })
    }

    override fun getAdapter(): BaseItemDraggableAdapter<TranslateRecord, BaseViewHolder> {
        mBaseQuickAdapter = RecordAdapter(mDatas)
        val onItemSwipeListener: OnItemSwipeListener = object : OnItemSwipeListener {
            override fun clearView(p0: RecyclerView.ViewHolder?, p1: Int) {
            }

            override fun onItemSwiped(p0: RecyclerView.ViewHolder?, p1: Int) {
            }

            override fun onItemSwipeStart(p0: RecyclerView.ViewHolder?, p1: Int) {
            }

            override fun onItemSwipeMoving(p0: Canvas?, p1: RecyclerView.ViewHolder?, p2: Float, p3: Float, p4: Boolean) {
            }

        }
        mBaseQuickAdapter.enableSwipeItem()
        mBaseQuickAdapter.setOnItemSwipeListener(onItemSwipeListener)
        return mBaseQuickAdapter
    }


    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_TYPE = "section_type"

        const val TYPE_TIME_DESC = 0
        const val TYPE_QUERY_NUM_ASC = 1

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(sectionNumber: Int): RecordListFragment {
            val fragment = RecordListFragment()
            val args = Bundle()
            args.putInt(ARG_SECTION_TYPE, sectionNumber)
            fragment.arguments = args
            return fragment
        }

    }

    class RecordAdapter(datas: List<TranslateRecord>) : BaseItemDraggableAdapter<TranslateRecord, BaseViewHolder>(R.layout.item_words, datas) {


        override fun convert(helper: BaseViewHolder, item: TranslateRecord?) {
            helper.setText(R.id.tv_words, item!!.words_text)
            helper.setText(R.id.tv_display_content, item.display_content)
            helper.setText(R.id.tv_query_num, item.quest_num.toString())
            helper.getView<LinearLayout>(R.id.right).setOnClickListener {


                val service: TranslateRecordService = RetrofitManager.instance.create(TranslateRecordService::class.java)
                syncWrok(service.delete(item.words_text!!), Consumer {
                    if (it.isResultOk()) {
                        // 列表层删除相关位置的数据
                        mData.removeAt(helper.layoutPosition)
                        // 更新视图
                        notifyItemRemoved(helper.layoutPosition)
                    } else {
                        showToast(mContext, "delete Failed")
                    }
                })

            }
        }
    }
}