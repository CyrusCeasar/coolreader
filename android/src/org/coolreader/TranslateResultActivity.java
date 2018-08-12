package org.coolreader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.youdao.sdk.ydtranslate.Translate;
import com.youdao.sdk.ydtranslate.TranslateErrorCode;
import com.youdao.sdk.ydtranslate.TranslateListener;

import org.coolreader.crengine.TranslateUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TranslateResultActivity extends Activity {


    private static final String KEY_INPUT = "key_input";


    private static final String TAG = TranslateResultActivity.class.getSimpleName();

    ProgressBar progressDialog;
    ListView listView;

    List<String> resutls = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String input = getIntent().getStringExtra(KEY_INPUT);
        setContentView(R.layout.activity_translate);

        findViewById(R.id.content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        progressDialog = findViewById(R.id.pb_loading);
        listView = findViewById(R.id.lv_reuslt);

        TranslateUtil.translate(input, new TranslateListener() {
            @Override
            public void onError(TranslateErrorCode translateErrorCode, String s) {
                resutls.add(translateErrorCode.toString());
                Log.d(TAG, translateErrorCode.toString() + translateErrorCode.getCode() + "      " + s);
                notifyDataUpdate();
            }

            @Override
            public void onResult(Translate translate, String s, String s1) {
                StringBuilder sb = new StringBuilder();

                if (translate.getExplains() != null && !translate.getExplains().isEmpty()) {
                    for (String content : translate.getExplains()) {
                        resutls.add(content);
                        sb.append(content);
                    }
                }

                if (translate.getTranslations() != null && !translate.getTranslations().isEmpty()) {
                    for (String content : translate.getTranslations()) {
                        resutls.add(content);
                        sb.append(content);
                    }

                }
                Log.d(TAG, sb.toString() + s + s1);
                final String url = String.format("http://45.78.12.192/translate_record/query?words=%s&src_content=%s&display_content=%s", input, "", sb.toString());
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        HttpUtil.get(url);
                    }
                }.start();
                notifyDataUpdate();
            }

            @Override
            public void onResult(List<Translate> list, List<String> list1, List<TranslateErrorCode> list2, String s) {

            }
        });

    }


    private void notifyDataUpdate() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                ArrayAdapter arrayAdapter = new ArrayAdapter(TranslateResultActivity.this, android.R.layout.simple_list_item_1, resutls);
                listView.setAdapter(arrayAdapter);
            }
        });

    }

    public static void startThis(String content, Context context) {
        Intent intent = new Intent();
        intent.setClass(context, TranslateResultActivity.class);
        intent.putExtra(KEY_INPUT, content);
        context.startActivity(intent);
    }


}
