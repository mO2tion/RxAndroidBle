package com.polidea.rxandroidble.helpers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.DaggerClientComponent;
import com.polidea.rxandroidble.internal.util.LocationServicesStatus;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Action;

/**
 * An Observable that emits false if an attempt to scan with {@link com.polidea.rxandroidble.RxBleClient#scanBleDevices(UUID...)}
 * would cause the exception {@link com.polidea.rxandroidble.exceptions.BleScanException#LOCATION_SERVICES_DISABLED}; otherwise emits true.
 * Always emits true in Android versions prior to 6.0.
 * Typically, receiving false should cause the user to be prompted to enable Location Services.
 */
public class LocationServicesOkObservable extends Observable<Boolean> {

    @NonNull
    private final Context context;
    @NonNull
    private final LocationServicesStatus locationServicesStatus;

    public static LocationServicesOkObservable createInstance(@NonNull final Context context) {
        return DaggerClientComponent
                .builder()
                .clientModule(new ClientComponent.ClientModule(context))
                .build()
                .locationServicesOkObservable();
    }

    @Inject
    LocationServicesOkObservable(@NonNull final Context context, @NonNull final LocationServicesStatus locationServicesStatus) {
        this.context = context;
        this.locationServicesStatus = locationServicesStatus;
    }

    @Override
    protected void subscribeActual(final Observer<? super Boolean> observer) {
        final boolean locationProviderOk = locationServicesStatus.isLocationProviderOk();
        final AtomicBoolean locationProviderOkAtomicBoolean = new AtomicBoolean(locationProviderOk);
        observer.onNext(locationProviderOk);

        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final boolean newLocationProviderOkValue = locationServicesStatus.isLocationProviderOk();
                final boolean valueChanged = locationProviderOkAtomicBoolean
                        .compareAndSet(!newLocationProviderOkValue, newLocationProviderOkValue);
                if (valueChanged) {
                    observer.onNext(newLocationProviderOkValue);
                }
            }
        };

        context.registerReceiver(broadcastReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        observer.onSubscribe(Disposables.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                context.unregisterReceiver(broadcastReceiver);
            }
        }));
    }
}
