package cn.cyrus.translater.feater

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import cn.cc.ereader.TranslateResultListFragment
import cn.cyrus.translater.base.BaseLazyInitFragment
import cn.cyrus.translater.base.showToast
import cn.cyrus.translater.base.uitls.inTransaction
import org.coolreader.R

class TranslateFragment : BaseLazyInitFragment() {


    val TAG = TranslateFragment::class.java.simpleName

    private lateinit var metInput: EditText
    private lateinit var btnClear: Button
    private lateinit var btnTranslate: Button
    private lateinit var etInput: EditText

   private val mTranslateResult: TranslateResultListFragment by lazy {
        TranslateResultListFragment(metInput.text.toString())
    }


    override fun initView(layoutInflater: LayoutInflater): View? {
        val view = layoutInflater.inflate(R.layout.fragment_translate, null)
        metInput = view.findViewById(R.id.et_input)
        btnClear = view.findViewById(R.id.btn_clear)
        btnTranslate = view.findViewById(R.id.btn_translate)
        etInput = view.findViewById(R.id.et_input)

        btnClear.setOnClickListener {
            etInput.setText("")
        }

        activity!!.supportFragmentManager.inTransaction {
            add(R.id.fl_container, mTranslateResult)
        }
        mTranslateResult.userVisibleHint = true


           btnTranslate.setOnClickListener {
               val input: String = etInput.text.toString()
               if (TextUtils.isEmpty(input)) {
                   showToast(context!!, "内容不能为空!")
               } else {
                   mTranslateResult.translate(input)
               }
           }
        return view
    }


}
