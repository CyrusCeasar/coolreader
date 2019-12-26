package cn.cyrus.translater.base

import com.google.gson.JsonObject
import io.reactivex.Observable
import retrofit2.http.*

/**
 * Created by ChenLei on 2018/8/18 0018.
 */
interface TranslateRecordService {

    @GET("translate_record/list")
    fun recordList(@Query("type") type:Int, @Query("page") page:Int):Observable<Result<ArrayList<TranslateRecord>>>

    @GET("translate_record/delete")
    fun delete(@Query("words") words:String):Observable<Result<Int>>

    @Headers("Content-Type: application/json;charset=utf-8")
    @POST("translate_record/lookup")
    fun  query(@Body data: JsonObject): Observable<Result<Any>>
}