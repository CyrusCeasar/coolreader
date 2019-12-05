package cn.cc.ereader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import cn.cyrus.translater.base.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.youdao.sdk.ydtranslate.Translate
import com.youdao.sdk.ydtranslate.TranslateErrorCode
import com.youdao.sdk.ydtranslate.TranslateListener
import io.reactivex.functions.Consumer
import org.coolreader.R
import org.coolreader.crengine.TranslateUtil
import java.util.*

class TranslateResultActivity : Activity() {
    var progressDialog: ProgressBar? = null
    var listView: ListView? = null
    var resutls: MutableList<String> = ArrayList()
    var gson = Gson()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val input = intent.getStringExtra(KEY_INPUT)
        setContentView(R.layout.activity_translate)
        findViewById<View>(R.id.content).setOnClickListener { finish() }
        progressDialog = findViewById(R.id.pb_loading)
        listView = findViewById(R.id.lv_reuslt)
        TranslateUtil.translate(input, object : TranslateListener {
            override fun onError(translateErrorCode: TranslateErrorCode, s: String) {
                resutls.add(translateErrorCode.toString())
                Log.d(TAG, translateErrorCode.toString() + translateErrorCode.code + "      " + s)
                notifyDataUpdate()
            }

            override fun onResult(translate: Translate, input: String, s1: String) {
                val sb = StringBuilder()
                if (!TextUtils.isEmpty(translate.phonetic)) {
                    resutls.add(translate.phonetic)
                }
                if (translate.explains != null && translate.explains.isNotEmpty()) {
                    for (content in translate.explains) {
                        resutls.add(content)
                        sb.append(content)
                    }
                }
                if (translate.translations != null && translate.translations.isNotEmpty()) {
                    for (content in translate.translations) {
                        resutls.add(content)
                        sb.append(content)
                    }
                }
                val jsonStr = Gson().toJson(translate, Translate::class.java)
                LogUtil.json(jsonStr)
                LruDiskUtil.save(input, jsonStr.toByteArray())//把数据缓存到本地

                val trs = sb.toString()
                val src = jsonStr
                val trss: TranslateService = RetrofitManager.instance.create(TranslateService::class.java)
                val data = JsonObject()
                data.addProperty("words",input)
                data.addProperty("src",src)
                data.addProperty("show_content",trs)
                syncWrok(trss.query(data), Consumer {
                    Log.d(TAG, "result ok" + it.isResultOk())
                })
                notifyDataUpdate()
            }

            override fun onResult(list: List<Translate>, list1: List<String>, list2: List<TranslateErrorCode>, s: String) {}
        })
    }

    private fun notifyDataUpdate() {
        if (resutls.isEmpty()) {
            return
        }
        runOnUiThread {
            progressDialog!!.visibility = View.GONE
            listView!!.visibility = View.VISIBLE
            val arrayAdapter: ArrayAdapter<*> = ArrayAdapter(this@TranslateResultActivity, android.R.layout.simple_list_item_1, resutls)
            listView!!.adapter = arrayAdapter
        }
    }

    companion object {
        private const val KEY_INPUT = "key_input"
        private val TAG = TranslateResultActivity::class.java.simpleName
        fun startThis(content: String?, context: Context) {
            val intent = Intent()
            intent.setClass(context, TranslateResultActivity::class.java)
            intent.putExtra(KEY_INPUT, content)
            context.startActivity(intent)
        }
    }
}