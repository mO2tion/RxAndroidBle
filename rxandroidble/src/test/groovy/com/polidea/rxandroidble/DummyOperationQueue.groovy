package com.polidea.rxandroidble

import com.polidea.rxandroidble.exceptions.BleException
import com.polidea.rxandroidble.internal.operations.Operation
import com.polidea.rxandroidble.internal.serialization.ConnectionOperationQueue
import com.polidea.rxandroidble.internal.util.DisposableUtil
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.annotations.NonNull
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer

class DummyOperationQueue implements ConnectionOperationQueue {
    public final MockSemaphore semaphore = new MockSemaphore()

    @Override
    def <T> Observable<T> queue(Operation<T> operation) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            void subscribe(@NonNull ObservableEmitter tEmitter) throws Exception {
                semaphore.awaitRelease()
                operation
                        .run(semaphore)
                        .doOnNext(new Consumer<Object>() {
                    @Override
                    void accept(Object t) throws Exception {
                        println("NExt")
                    }
                })
                        .doOnError(new Consumer<Object>() {
                    @Override
                    void accept(Object t) throws Exception {
                        println("err")
                    }
                })
                        .doOnComplete(new Action() {
                    @Override
                    void run() throws Exception {
                        println("Complete")
                    }
                })
                        .doFinally(new Action() {
                    @Override
                    void run() throws Exception {
                        println("finally")
                    }
                })
                        .subscribeWith(DisposableUtil.disposableEmitter(tEmitter))
            }
        })
    }

    @Override
    void terminate(BleException disconnectException) {
        // do nothing
    }
}
