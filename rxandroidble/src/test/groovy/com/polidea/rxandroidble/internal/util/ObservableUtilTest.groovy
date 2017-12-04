package com.polidea.rxandroidble.internal.util

import io.reactivex.subscribers.TestSubscriber
import spock.lang.Specification

class ObservableUtilTest extends Specification {

    def "should pass through parameter on subscribe and not complete"() {
        given:
        String someObject = "someText"
        TestSubscriber testSubscriber = new TestSubscriber()

        when:
        ObservableUtil.justOnNext(someObject).test()

        then:
        testSubscriber.assertValue(someObject)

        and:
        testSubscriber.assertNotCompleted()
    }
}
