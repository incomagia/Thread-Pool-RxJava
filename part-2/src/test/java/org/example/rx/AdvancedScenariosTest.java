package org.example.rx;

import org.example.rx.schedulers.ComputationScheduler;
import org.example.rx.schedulers.IOThreadScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки сложных сценариев работы реактивного программирования
 */
class AdvancedScenariosTest {
    /**
     * Тест проверяет работу с пустым Observable
     * - Создается Observable без эмиссии значений
     * - Проверяется, что не получено ни одного значения
     * - Проверяется корректное завершение
     */
    @Test
    void testEmptyObservable() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger receivedCount = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable.create(observer -> {
            observer.onComplete();
        })
        .subscribe(
            item -> receivedCount.incrementAndGet(),
            error -> fail("Unexpected error"),
            () -> {
                completed.set(true);
                latch.countDown();
            }
        );

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(0, receivedCount.get());
        assertTrue(completed.get());
    }

    /**
     * Тест проверяет работу с Observable, эмитящим одно значение
     * - Создается Observable с одним значением 42
     * - Проверяется получение этого значения
     * - Проверяется корректное завершение
     */
    @Test
    void testSingleValueObservable() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> receivedValue = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable.<Integer>create(observer -> {
            observer.onNext(42);
            observer.onComplete();
        })
        .subscribe(
            item -> receivedValue.set(item),
            error -> fail("Unexpected error"),
            () -> {
                completed.set(true);
                latch.countDown();
            }
        );

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(42, receivedValue.get());
        assertTrue(completed.get());
    }

    /**
     * Тест проверяет обработку большого количества значений
     * - Создается Observable с 10000 значениями
     * - Проверяется получение всех значений
     * - Устанавливается таймаут в 2 секунды
     */
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testLargeNumberOfValues() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger receivedCount = new AtomicInteger(0);
        final int expectedCount = 10000;

        Observable.<Integer>create(observer -> {
            for (int i = 0; i < expectedCount; i++) {
                observer.onNext(i);
            }
            observer.onComplete();
        })
        .subscribe(
            item -> receivedCount.incrementAndGet(),
            error -> fail("Unexpected error"),
            () -> latch.countDown()
        );

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(expectedCount, receivedCount.get());
    }

    /**
     * Тест проверяет работу с длительными операциями
     * - Создается Observable с задержкой 100мс
     * - Проверяется, что операция выполняется не менее 100мс
     * - Используется IOThreadScheduler для выполнения в отдельном потоке
     */
    @Test
    void testLongRunningOperation() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong startTime = new AtomicLong();
        AtomicLong endTime = new AtomicLong();

        Observable.<Integer>create(observer -> {
                    startTime.set(System.currentTimeMillis());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        observer.onError(e);
                        Thread.currentThread().interrupt(); // корректное поведение при прерывании
                        return;
                    }
                    observer.onNext(1);
                    observer.onComplete();
        })

        .subscribeOn(new IOThreadScheduler())
        .subscribe(
            item -> {},
            error -> fail("Unexpected error"),
            () -> {
                endTime.set(System.currentTimeMillis());
                latch.countDown();
            }
        );

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(endTime.get() - startTime.get() >= 100);
    }

    /**
     * Тест проверяет вложенные подписки
     * - Создается внешний Observable
     * - Внутри него создается и подписывается внутренний Observable
     * - Проверяется порядок получения значений
     */
    @Test
    void testNestedSubscriptions() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<Integer> receivedValues = new ArrayList<>();

        Observable.<Integer>create(outerObserver -> {
            outerObserver.onNext(1);
            Observable.<Integer>create(innerObserver -> {
                innerObserver.onNext(2);
                innerObserver.onComplete();
            })
            .subscribe(
                item -> outerObserver.onNext(item),
                error -> outerObserver.onError(error),
                () -> {
                    outerObserver.onNext(3);
                    outerObserver.onComplete();
                }
            );
        })
        .subscribe(
            item -> receivedValues.add(item),
            error -> fail("Unexpected error"),
            () -> latch.countDown()
        );

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(3, receivedValues.size());
        assertEquals(1, receivedValues.get(0));
        assertEquals(2, receivedValues.get(1));
        assertEquals(3, receivedValues.get(2));
    }

    /**
     * Тест проверяет сложные цепочки операторов
     * - Создается Observable с числами от 0 до 9
     * - Применяется filter для четных чисел
     * - Применяется map для умножения на 2
     * - Применяется flatMap для создания пар чисел
     * - Проверяется результат преобразований
     */
    @Test
    void testComplexOperatorChain() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<Integer> receivedValues = new ArrayList<>();

        Observable.<Integer>create(observer -> {
            for (int i = 0; i < 10; i++) {
                observer.onNext(i);
            }
            observer.onComplete();
        })
        .filter(x -> x % 2 == 0)
        .map(x -> x * 2)
        .flatMap(x -> Observable.<Integer>create(observer -> {
            observer.onNext(x);
            observer.onNext(x + 1);
            observer.onComplete();
        }))
        .subscribe(
            item -> receivedValues.add(item),
            error -> fail("Unexpected error"),
            () -> latch.countDown()
        );

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(10, receivedValues.size());
        assertEquals(0, receivedValues.get(0));
        assertEquals(1, receivedValues.get(1));
        assertEquals(4, receivedValues.get(2));
        assertEquals(5, receivedValues.get(3));
    }

    /**
     * Тест проверяет обработку специфических ошибок
     * - Создается Observable, выбрасывающий IllegalStateException
     * - Проверяется корректное получение ошибки
     * - Проверяется тип и сообщение ошибки
     */
    @Test
    void testSpecificErrorHandling() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        String errorMessage = "Specific error";

        Observable.<Integer>create(observer -> {
            throw new IllegalStateException(errorMessage);
        })
        .subscribeOn(new IOThreadScheduler())
        .map(x -> {
            throw new RuntimeException("Should not reach here");
        })
        .subscribe(
            item -> fail("Should not receive items"),
            error -> {
                receivedError.set(error);
                latch.countDown();
            },
            () -> fail("Should not complete")
        );

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(receivedError.get());
        assertEquals(IllegalStateException.class, receivedError.get().getClass());
        assertEquals(errorMessage, receivedError.get().getMessage());
    }

    /**
     * Тест проверяет конкурентные подписки
     * - Создается один Observable
     * - На него подписываются два разных планировщика
     * - Проверяется корректное получение всех значений
     */
    @Test
    void testConcurrentSubscriptions() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger totalReceived = new AtomicInteger(0);
        final int expectedPerSubscription = 1000;

        Observable<Integer> source = Observable.create(observer -> {
            for (int i = 0; i < expectedPerSubscription; i++) {
                observer.onNext(i);
            }
            observer.onComplete();
        });

        source.subscribeOn(new IOThreadScheduler())
            .subscribe(
                item -> totalReceived.incrementAndGet(),
                error -> fail("Unexpected error"),
                () -> latch.countDown()
            );

        source.subscribeOn(new ComputationScheduler())
            .subscribe(
                item -> totalReceived.incrementAndGet(),
                error -> fail("Unexpected error"),
                () -> latch.countDown()
            );

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(expectedPerSubscription * 2, totalReceived.get());
    }
} 