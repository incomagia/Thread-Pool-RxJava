package org.example.rx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ObservableTest {

    @Test
    void testBasicEmission() {
        List<Integer> received = new ArrayList<>();

        Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onNext(3);
            observer.onComplete();
        }).subscribe(
                item -> received.add((Integer) item),
                error -> fail("Unexpected error"),
                () -> {}
        );

        assertEquals(3, received.size());
        assertEquals(1, received.get(0));
        assertEquals(2, received.get(1));
        assertEquals(3, received.get(2));
    }

    @Test
    void testErrorInObservable() {
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        String errorMessage = "Test exception";

        Observable.create(observer -> {
            observer.onNext(1);
            throw new RuntimeException(errorMessage);
        }).subscribe(
                item -> assertEquals(1, (int) item),
                error -> receivedError.set(error),
                () -> fail("Should not complete")
        );

        assertNotNull(receivedError.get());
        assertEquals(errorMessage, receivedError.get().getMessage());
    }

    @Test
    void testMapOperator() {
        List<Integer> received = new ArrayList<>();

        Observable.create(observer -> {
                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onNext(3);
                    observer.onComplete();
                })
                .map(x -> ((Integer) x) * 10)
                .subscribe(
                        item -> received.add((Integer) item),
                        error -> fail("Unexpected error"),
                        () -> {}
                );

        assertEquals(3, received.size());
        assertEquals(10, received.get(0));
        assertEquals(20, received.get(1));
        assertEquals(30, received.get(2));
    }

    @Test
    void testFilterOperator() {
        List<Integer> received = new ArrayList<>();

        Observable.create(observer -> {
                    for (int i = 0; i < 5; i++) {
                        observer.onNext(i);
                    }
                    observer.onComplete();
                })
                .filter(x -> ((Integer) x) % 2 == 0)
                .subscribe(
                        item -> received.add((Integer) item),
                        error -> fail("Unexpected error"),
                        () -> {}
                );

        assertEquals(3, received.size());
        assertEquals(0, received.get(0));
        assertEquals(2, received.get(1));
        assertEquals(4, received.get(2));
    }

    @Test
    void testEmptyObservableCompletion() {
        AtomicInteger completeCalled = new AtomicInteger(0);

        Observable.create(observer -> {
            observer.onComplete();
        }).subscribe(
                item -> fail("No items should be emitted"),
                error -> fail("Unexpected error"),
                () -> completeCalled.incrementAndGet()
        );

        assertEquals(1, completeCalled.get());
    }
}
