package cn.cyrus.translater.feater

import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import cn.cyrus.translater.base.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.youdao.sdk.ydtranslate.Translate
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.line_edit_dlg.*
import org.coolreader.R

class TranslateFragment : BaseLazyInitFragment() {


    val TAG = TranslateFragment::class.java.simpleName

    private lateinit var metInput: EditText
    private lateinit var mlvResults: ListView
    private val results: ArrayList<String> = ArrayList()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var btnClear: Button
    private lateinit var btnTranslate: Button
    private lateinit var etInput: EditText
    private lateinit var mAudioManager: AudioManager

    private val callback: (Translate, String) -> Unit = { p0, p1 ->
        results.clear()
        val sb = StringBuilder()

        val phoneticStr = "uk:" + p0.ukPhonetic

        results.add(p0.ukSpeakUrl)
        results.add(phoneticStr)
        if (p0.explains != null && !p0.explains.isEmpty()) {
            for (content in p0.explains) {
                results.add(content)
                sb.append(content)
            }
        }

        if (p0.translations != null && !p0.translations.isEmpty()) {
            for (content in p0.translations) {
                results.add(content)
                sb.append(content)
            }
        }


        val jsonStr = Gson().toJson(p0, Translate::class.java)
        LogUtil.json(jsonStr)
        LruDiskUtil.save(p1, jsonStr.toByteArray())//把数据缓存到本地

        val trs = sb.toString()
        val src = jsonStr
        val trss: TranslateService = RetrofitManager.instance.create(TranslateService::class.java)
        val data = JsonObject()
        data.addProperty("words",p1)
        data.addProperty("src",src)
        data.addProperty("show_content",trs)
        syncWrok(trss.query(data), Consumer {
            Log.d(TAG, "result ok" + it.isResultOk())
        })
        notifyDataChange()
    }

    override fun initView(layoutInflater: LayoutInflater): View? {
        val view = layoutInflater.inflate(R.layout.fragment_translate, null)
        metInput = view.findViewById(R.id.et_input)
        mlvResults = view.findViewById(R.id.lv_results)
        btnClear = view.findViewById(R.id.btn_clear)
        btnTranslate = view.findViewById(R.id.btn_translate)
        etInput = view.findViewById(R.id.et_input)
        adapter = ArrayAdapter(activity!!, android.R.layout.simple_list_item_1, results)
        mlvResults.adapter = adapter

        btnClear.setOnClickListener {
            etInput.setText("")
        }

        mlvResults.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            if (position == 0 && results[position].contains("http:")) {
                mAudioManager.play(results[0])
            }
        }

        mAudioManager = AudioManager()

        btnTranslate.setOnClickListener { _ ->
            val input: String = etInput.text.toString()
            if (TextUtils.isEmpty(input)) {
                showToast(context!!, "内容不能为空!")
            } else {
                LruDiskUtil.get(input) {
                    if (it == null || it.isEmpty()) {
                        TranslateUtil.translate(input, callback) { p0, _ ->
                            results.clear()
                            results.add(p0.toString())
                            notifyDataChange()
                        }
                    } else {
                        val trans: Translate = Gson().fromJson(String(it, 0, it.size), Translate::class.java)
                        callback.invoke(trans, input)
                    }
                }


            }
        }
        return view
    }


    fun notifyDataChange() {
        activity!!.runOnUiThread {
            adapter.notifyDataSetChanged()
        }
    }
}
