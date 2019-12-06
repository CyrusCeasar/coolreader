package cn.cyrus.translater.base

import com.google.gson.JsonObject
import io.reactivex.Observable
import retrofit2.http.*

/**
 * Created by ChenLei on 2018/8/18 0018.
 */
interface TranslateService {

    @Headers("Content-Type: application/json;charset=utf-8")
    @POST("translate_record/query")
    fun  query(@Body data:JsonObject): Observable<Result<Any>>

}