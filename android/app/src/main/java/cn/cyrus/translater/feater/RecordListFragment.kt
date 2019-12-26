package cn.cyrus.translater.feater

import android.os.Bundle
import cn.cyrus.translater.base.*
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import io.reactivex.functions.Consumer
import org.coolreader.R

/**
 * Created by ChenLei on 2018/8/21 0021.
 */
/**
 * A placeholder fragment containing a simple view.
 */
class RecordListFragment : DefaultLoadMoreFragment<TranslateRecord>() {
    override fun load(onUpdate: (List<TranslateRecord>?) -> Unit) {
        mPageManager.reset()
        syncWrok(serv.recordList(arguments!!.getInt(ARG_SECTION_TYPE), mPageManager.pageIndicator!!), Consumer {
            if (it.isResultOk()) {
                onUpdateListener.invoke(it.data)
            } else {
                updateError()
            }
        })
    }

    private val serv: TranslateRecordService = RetrofitManager.instance.create(TranslateRecordService::class.java)
    override fun loadMore(onLoadMore: (List<TranslateRecord>?) -> Unit) {
        syncWrok(serv.recordList(1, mPageManager.pageIndicator!!), Consumer {
            if (it.isResultOk()) {
                onLoadMore.invoke(it.data)
            } else {
                loadMoreError()
            }
        })
    }

    override fun getAdapter(): BaseQuickAdapter<TranslateRecord, BaseViewHolder> {
        mBaseQuickAdapter = RecordAdapter(mDatas)
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

    class RecordAdapter(datas: List<TranslateRecord>) : BaseQuickAdapter<TranslateRecord, BaseViewHolder>(R.layout.item_words, datas) {


        override fun convert(helper: BaseViewHolder, item: TranslateRecord?) {
            helper.setText(R.id.tv_words, item!!.vocabulary__word)
            helper.setText(R.id.tv_display_content, item.vocabulary__display_content)
            helper.setText(R.id.tv_query_num, item.lookup_amount.toString())
        }
    }
}