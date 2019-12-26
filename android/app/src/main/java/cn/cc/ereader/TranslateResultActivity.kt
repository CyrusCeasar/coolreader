package cn.cc.ereader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cn.cyrus.translater.base.uitls.addFragment
import org.coolreader.R

class TranslateResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val input = intent.getStringExtra(KEY_INPUT)
        setContentView(R.layout.activity_translate)

        val fg = TranslateResultListFragment(input)
        addFragment(fg, R.id.fl_container)

        fg.userVisibleHint = true
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