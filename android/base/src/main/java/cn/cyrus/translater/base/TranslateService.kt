package cn.cyrus.translater.base

import com.google.gson.JsonObject
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Created by ChenLei on 2018/8/18 0018.
 */
interface TranslateService {

    @POST("translate_record/query")
    fun  query(@Body data:JsonObject): Observable<Result<Any>>

}