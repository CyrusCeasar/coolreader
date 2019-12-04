package cn.cyrus.translater.base

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by ChenLei on 2018/8/18 0018.
 */
class RetrofitManager {
    companion object {
        val instance: Retrofit = Retrofit.Builder().baseUrl("http://103.91.67.151/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(OkHttpClient().newBuilder().addNetworkInterceptor(HttpLoggingInterceptor(HttpLogger()).setLevel(HttpLoggingInterceptor.Level.BODY)).build()).build()
    }

    class HttpLogger : HttpLoggingInterceptor.Logger {
        override fun log(message: String?) {
            LogUtil.d( message!!)
        }

    }


}
