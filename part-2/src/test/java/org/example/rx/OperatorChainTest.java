package org.example.rx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OperatorChainTest {
    @Test
    void testOperatorChain() {
        List<Integer> received = new ArrayList<>();

        Observable.create(observer -> {
                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onNext(3);
                    observer.onNext(4);
                    observer.onComplete();
                })
                .filter(x -> ((Integer) x) % 2 == 0)
                .map(x -> ((Integer) x) * 2)
                .subscribe(
                        item -> received.add((Integer) item),
                        error -> fail("Unexpected error"),
                        () -> {}
                );

        assertEquals(2, received.size());
        assertEquals(4, received.get(0));  // 2 * 2
        assertEquals(8, received.get(1));  // 4 * 2
    }

    @Test
    void testErrorPropagation() {
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        String errorMessage = "Test error";

        Observable.create(observer -> {
                    observer.onNext(1);
                    throw new RuntimeException(errorMessage);
                })
                .map(x -> ((Integer) x) * 2)
                .filter(x -> true)
                .subscribe(
                        item -> fail("Should not receive items after error"),
                        error -> receivedError.set(error),
                        () -> fail("Should not complete")
                );

        assertNotNull(receivedError.get());
        assertEquals(errorMessage, receivedError.get().getMessage());
    }

    @Test
    void testFlatMapErrorHandling() {
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        String errorMessage = "FlatMap error";

        Observable.create(observer -> {
                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onComplete();
                })
                .flatMap(x -> Observable.create(observer -> {
                    if (((Integer) x) == 2) {
                        throw new RuntimeException(errorMessage);
                    }
                    observer.onNext(((Integer) x) * 10);
                    observer.onComplete();
                }))
                .subscribe(
                        item -> assertEquals(10, (int) item),  // Only first item should be received
                        error -> receivedError.set(error),
                        () -> fail("Should not complete due to error")
                );

        assertNotNull(receivedError.get());
        assertEquals(errorMessage, receivedError.get().getMessage());
    }

    @Test
    void testDisposableInChain() {
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean disposed = new AtomicBoolean(false);

        Observable<Integer> source = Observable.create(observer -> {
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

        Disposable subscription = source
                .filter(x -> ((Integer) x) % 2 == 0)
                .map(x -> ((Integer) x) * 2)
                .subscribe(
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

    @Test
    void testComplexOperatorChain() {
        List<Integer> received = new ArrayList<>();

        Observable.create(observer -> {
                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onNext(3);
                    observer.onNext(4);
                    observer.onNext(5);
                    observer.onComplete();
                })
                .filter(x -> ((Integer) x) > 1)  // Remove 1
                .map(x -> ((Integer) x) * 2)     // Double remaining values
                .filter(x -> ((Integer) x) % 3 == 0)  // Keep only multiples of 3
                .flatMap(x -> Observable.create(observer -> {
                    observer.onNext(x);
                    observer.onNext(((Integer) x) * 10);
                    observer.onComplete();
                }))
                .subscribe(
                        item -> received.add((Integer) item),
                        error -> fail("Unexpected error"),
                        () -> {}
                );

        assertEquals(4, received.size());
        // Expected sequence: 6, 60 (from 3), 12, 120 (from 6)
        assertEquals(6, received.get(0));
        assertEquals(60, received.get(1));
        assertEquals(12, received.get(2));
        assertEquals(120, received.get(3));
    }

    @Test
    void testErrorInOperatorChain() {
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        String errorMessage = "Operator error";

        Observable.create(observer -> {
                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onNext(3);
                    observer.onComplete();
                })
                .map(x -> {
                    if (((Integer) x) == 2) {
                        throw new RuntimeException(errorMessage);
                    }
                    return ((Integer) x) * 2;
                })
                .filter(x -> ((Integer) x) > 0)
                .subscribe(
                        item -> assertEquals(2, (int) item),  // Only first item should be received
                        error -> receivedError.set(error),
                        () -> fail("Should not complete due to error")
                );

        assertNotNull(receivedError.get());
        assertEquals(errorMessage, receivedError.get().getMessage());
    }

    @Test
    void testDisposableWithMultipleOperators() {
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean disposed = new AtomicBoolean(false);

        Observable<Integer> source = Observable.create(observer -> {
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

        Disposable subscription = source
                .filter(x -> ((Integer) x) % 2 == 0)
                .map(x -> ((Integer) x) * 2)
                .flatMap(x -> Observable.create(observer -> {
                    observer.onNext(x);
                    observer.onNext(((Integer) x) * 10);
                    observer.onComplete();
                }))
                .subscribe(
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