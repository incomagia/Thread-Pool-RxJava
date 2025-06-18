package org.example.rx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ObservableTestFixed {
    @Test
    void testBasicEmission() {
        List<Integer> received = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onNext(3);
            observer.onComplete();
        });

        observable.subscribe(
                item -> received.add((Integer) item),
                error -> fail("Unexpected error: " + error.getMessage()),
                () -> completed.set(true)
        );

        assertEquals(3, received.size());
        assertEquals(1, received.get(0));
        assertEquals(2, received.get(1));
        assertEquals(3, received.get(2));
        assertTrue(completed.get());
    }

    @Test
    void testErrorHandling() {
        AtomicBoolean errorReceived = new AtomicBoolean(false);
        String errorMessage = "Test error";

        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            throw new RuntimeException(errorMessage);
        });

        observable.subscribe(
                item -> {},
                error -> {
                    errorReceived.set(true);
                    assertEquals(errorMessage, error.getMessage());
                },
                () -> fail("Should not complete")
        );

        assertTrue(errorReceived.get());
    }

    @Test
    void testMapOperator() {
        List<Integer> received = new ArrayList<>();

        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onComplete();
        });

        observable.map(x -> ((Integer) x) * 2)
                .subscribe(
                        item -> received.add((Integer) item),
                        error -> fail("Unexpected error"),
                        () -> {}
                );

        assertEquals(2, received.size());
        assertEquals(2, received.get(0));
        assertEquals(4, received.get(1));
    }

    @Test
    void testFilterOperator() {
        List<Integer> received = new ArrayList<>();

        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onNext(3);
            observer.onComplete();
        });

        observable.filter(x -> ((Integer) x) % 2 == 0)
                .subscribe(
                        item -> received.add((Integer) item),
                        error -> fail("Unexpected error"),
                        () -> {}
                );

        assertEquals(1, received.size());
        assertEquals(2, received.get(0));
    }

    @Test
    void testFlatMapOperator() {
        List<Integer> received = new ArrayList<>();

        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onComplete();
        });

        observable.flatMap(x -> Observable.create(observer -> {
                    observer.onNext(((Integer) x) * 10);
                    observer.onNext(((Integer) x) * 20);
                    observer.onComplete();
                }))
                .subscribe(
                        item -> received.add((Integer) item),
                        error -> fail("Unexpected error"),
                        () -> {}
                );

        assertEquals(4, received.size());
        assertEquals(10, received.get(0));
        assertEquals(20, received.get(1));
        assertEquals(20, received.get(2));
        assertEquals(40, received.get(3));
    }

    @Test
    void testDisposable() {
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean disposed = new AtomicBoolean(false);

        Observable<Integer> observable = Observable.create(observer -> {
            while (!disposed.get()) {
                observer.onNext(count.incrementAndGet());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        Disposable subscription = observable.subscribe(
                item -> {},
                error -> fail("Unexpected error"),
                () -> fail("Should not complete")
        );

        try {
            Thread.sleep(500);
            subscription.dispose();
            disposed.set(true);
            int finalCount = count.get();
            Thread.sleep(200);
            assertEquals(finalCount, count.get(), "Count should not increase after disposal");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }
}
