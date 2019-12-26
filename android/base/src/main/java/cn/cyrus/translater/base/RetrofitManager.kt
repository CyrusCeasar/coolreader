package cn.cyrus.translater.base

import cn.cyrus.translater.base.uitls.getDeviceId
import cn.cyrus.translater.base.uitls.logd
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.nio.charset.Charset

/**
 * Created by ChenLei on 2018/8/18 0018.
 */
class RetrofitManager {
    companion object {
        val instance: Retrofit = Retrofit.Builder().baseUrl(httpUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(OkHttpClient().newBuilder()
                        .addInterceptor(HeaderInterceptor())
                        .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                        .build())
                .build()
    }

    class HeaderInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                    .addHeader("Device-Id", getDeviceId(BaseApplication.INSANCE))
                    .addHeader("Device-Type", "android")
                    .build()
            return chain.proceed(request)
        }

    }

   /* class HttpLogger : HttpLoggingInterceptor.Logger {
        override fun log(message: String?) {

            logd(message!!)
        }

    }*/


}
