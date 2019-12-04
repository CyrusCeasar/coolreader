package cn.cyrus.translater.base

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers

/**
 * Created by ChenLei on 2018/8/18 0018.
 */

fun <T> syncWrok(task: Observable<Result<T>>, consumer: Consumer<Result<T>>, onError: Consumer<Throwable?> =  Consumer<Throwable?> {
        it?.let {
            it.message?.let { it1 ->
                LogUtil.e(it1)
            }
    }

}) {
    task.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : Observer<Result<T>> {
        override fun onSubscribe(d: Disposable) {
        }

        override fun onNext(t: Result<T>) {
            consumer.accept(t)
        }

        override fun onError(e: Throwable) {
            onError.accept(e)
        }

        override fun onComplete() {
        }

    })
}