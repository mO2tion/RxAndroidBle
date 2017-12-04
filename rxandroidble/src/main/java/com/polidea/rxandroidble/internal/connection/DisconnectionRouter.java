package com.polidea.rxandroidble.internal.connection;


import com.jakewharton.rxrelay2.PublishRelay;
import com.polidea.rxandroidble.RxBleAdapterStateObservable;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleGattException;
import com.polidea.rxandroidble.internal.DeviceModule;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;


/**
 * A class that is responsible for routing all potential sources of disconnection to an Observable that emits only errors.
 */
@ConnectionScope
class DisconnectionRouter implements DisconnectionRouterInput, DisconnectionRouterOutput {

    private final PublishRelay<BleException> disconnectionErrorInputRelay = PublishRelay.create();
    private final Observable<BleException> disconnectionErrorOutputObservable;

    @Inject
    DisconnectionRouter(
            @Named(DeviceModule.MAC_ADDRESS) final String macAddress,
            final RxBleAdapterWrapper adapterWrapper,
            final Observable<RxBleAdapterStateObservable.BleAdapterState> adapterStateObservable
    ) {
        final Observable<BleException> emitErrorWhenAdapterIsDisabled = awaitAdapterNotUsable(adapterWrapper, adapterStateObservable)
                .map(new Function<Boolean, BleException>() {
                    @Override
                    public BleException apply(Boolean isAdapterUsable) {
                        return new BleDisconnectedException(macAddress); // TODO: Introduce BleDisabledException?
                    }
                });

        disconnectionErrorOutputObservable = Observable.merge(
                disconnectionErrorInputRelay,
                emitErrorWhenAdapterIsDisabled
        )
                .replay()
                .autoConnect(0);
    }

    private static Observable<Boolean> awaitAdapterNotUsable(RxBleAdapterWrapper adapterWrapper,
                                                         Observable<RxBleAdapterStateObservable.BleAdapterState> stateChanges) {
        return stateChanges
                .map(new Function<RxBleAdapterStateObservable.BleAdapterState, Boolean>() {
                    @Override
                    public Boolean apply(RxBleAdapterStateObservable.BleAdapterState bleAdapterState) {
                        return bleAdapterState.isUsable();
                    }
                })
                .startWith(adapterWrapper.isBluetoothEnabled())
                .filter(new Predicate<Boolean>() {
                    @Override
                    public boolean test(Boolean isAdapterUsable) {
                        return !isAdapterUsable;
                    }
                });
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onDisconnectedException(BleDisconnectedException disconnectedException) {
        disconnectionErrorInputRelay.accept(disconnectedException);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onGattConnectionStateException(BleGattException disconnectedGattException) {
        disconnectionErrorInputRelay.accept(disconnectedGattException);
    }

    /**
     * @inheritDoc
     */
    @Override
    public Observable<BleException> asValueOnlyObservable() {
        return disconnectionErrorOutputObservable;
    }

    /**
     * @inheritDoc
     */
    @Override
    public <T> Observable<T> asErrorOnlyObservable() {
        return disconnectionErrorOutputObservable
                .flatMap(new Function<BleException, Observable<T>>() {
                    @Override
                    public Observable<T> apply(BleException e) {
                        return Observable.error(e);
                    }
                });
    }
}
