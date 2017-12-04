package com.polidea.rxandroidble.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import com.polidea.rxandroidble.internal.util.LocationServicesStatus
import org.robolectric.annotation.Config
import org.robospock.RoboSpecification

@Config(manifest = Config.NONE)
class LocationServicesOkObservableTest extends RoboSpecification {
    def contextMock = Mock Context
    def mockLocationServicesStatus = Mock LocationServicesStatus
    def objectUnderTest = new LocationServicesOkObservable(contextMock, mockLocationServicesStatus)
    BroadcastReceiver registeredReceiver

    def setup() {
        contextMock.getApplicationContext() >> contextMock
    }

    def "should register to correct receiver on subscribe"() {

        given:
        mockLocationServicesStatus.isLocationProviderOk() >> true

        when:
        objectUnderTest.subscribe()

        then:
        1 * contextMock.registerReceiver(!null, {
            it.hasAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        })
    }

    def "should unregister after observable was unsubscribed"() {

        given:
        mockLocationServicesStatus.isLocationProviderOk() >> true
        shouldCaptureRegisteredReceiver()
        def disposable = objectUnderTest.test()

        when:
        disposable.dispose()

        then:
        1 * contextMock.unregisterReceiver(registeredReceiver)
    }

    def "should emit what LocationServicesStatus.isLocationProviderOk() returns on subscribe and on next broadcasts"() {

        given:
        shouldCaptureRegisteredReceiver()
        mockLocationServicesStatus.isLocationProviderOk() >>> [true, false, true]

        when:
        def testObserver = objectUnderTest.test()

        then:
        testObserver.assertValue(true)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValues(true, false)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValues(true, false, true)
    }

    def "should not emit what LocationServicesStatus.isLocationProviderOk() returns on next broadcasts if the value does not change"() {

        given:
        shouldCaptureRegisteredReceiver()
        mockLocationServicesStatus.isLocationProviderOk() >>> [false, false, true, true, false, false]

        when:
        def testObserver = objectUnderTest.test()

        then:
        testObserver.assertValue(false)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValue(false)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValues(false, true)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValues(false, true)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValues(false, true, false)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValues(false, true, false)
    }

    public postStateChangeBroadcast() {
        def intent = new Intent(LocationManager.PROVIDERS_CHANGED_ACTION)
        registeredReceiver.onReceive(contextMock, intent)
    }

    public BroadcastReceiver shouldCaptureRegisteredReceiver() {
        _ * contextMock.registerReceiver({
            BroadcastReceiver receiver ->
                this.registeredReceiver = receiver
        }, _)
    }
}
