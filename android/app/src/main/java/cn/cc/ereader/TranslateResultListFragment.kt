package cn.cc.ereader

import android.R
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.cyrus.translater.base.*
import cn.cyrus.translater.base.uitls.*
import cn.cyrus.translater.feater.TranslateUtil
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.youdao.sdk.ydtranslate.Translate
import io.reactivex.functions.Consumer

class TranslateResultListFragment(src: String) : BaseLazyInitFragment() {


    val mTranslateResults = ArrayList<String>()
    lateinit var rc: RecyclerView

    private val transalteContent: String = src


    private fun notifyDataUpdate() {
        if (mTranslateResults.isEmpty()) {
            return
        }
        Dispatch.asyncOnMain {
            rc.adapter!!.notifyDataSetChanged()
        }

    }

    override fun initView(layoutInflater: LayoutInflater): View? {
        rc = RecyclerView(activity!!)
        rc.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        rc.adapter = object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.simple_list_item_1, mTranslateResults) {
            override fun convert(helper: BaseViewHolder, item: String?) {
                helper.setText(R.id.text1, item)
            }
        }
        translate(transalteContent)


        return rc
    }


    fun translate(str: String) {
        if (TextUtils.isEmpty(str)) {
            return
        }
        mTranslateResults.clear()
        val callback = { translate: Translate, _: String ->
            val sb = StringBuilder()
            if (!TextUtils.isEmpty(translate.phonetic)) {
                mTranslateResults.add(translate.phonetic)
                sb.append(translate.phonetic)
            }
            if (translate.explains != null && translate.explains.isNotEmpty()) {
                for (content in translate.explains) {
                    mTranslateResults.add(content)
                    sb.append(content)
                }
            }
            if (translate.translations != null && translate.translations.isNotEmpty()) {
                for (content in translate.translations) {
                    mTranslateResults.add(content)
                    sb.append(content)
                }
            }

            val jsonStr = GsonBuilder().disableHtmlEscaping().create().toJson(translate, Translate::class.java)
            logd(jsonStr)
            LruDiskUtil.save(str, jsonStr.toByteArray())//把数据缓存到本地

            val trs = sb.toString()
            val s = jsonStr
            val trss: TranslateRecordService = RetrofitManager.instance.create(TranslateRecordService::class.java)
            val data = JsonObject()
            data.addProperty("word", str)
            data.addProperty("src_content", s)
            data.addProperty("display_content", trs)
            syncWrok(trss.query(data), Consumer {
                logd("result ok" + it.isResultOk())
            })
            notifyDataUpdate()
        }
        LruDiskUtil.get(str) {
            if (it == null || it.isEmpty()) {
                TranslateUtil.translate(str, callback, { translateErrorCode, s ->
                    mTranslateResults.add(translateErrorCode.toString())
                    loge(translateErrorCode.toString() + translateErrorCode.code + "      " + s)
                    notifyDataUpdate()
                })
            } else {
                val trans: Translate = Gson().fromJson(String(it, 0, it.size), Translate::class.java)
                callback.invoke(trans, str)
            }
        }

    }

}