package org.example;

import org.example.rx.Disposable;
import org.example.rx.Observable;
import org.example.rx.schedulers.ComputationScheduler;
import org.example.rx.schedulers.IOThreadScheduler;
import org.example.rx.schedulers.SingleThreadScheduler;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        // Create schedulers
        IOThreadScheduler ioScheduler = new IOThreadScheduler();
        ComputationScheduler computationScheduler = new ComputationScheduler();
        SingleThreadScheduler singleThreadScheduler = new SingleThreadScheduler();

        // Example 1: Using flatMap to transform and flatten
        System.out.println("\nExample 1: Using flatMap");
        Observable<Integer> numbers = Observable.create(observer -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    observer.onNext(i);
                }
                observer.onComplete();
            } catch (Exception e) {
                observer.onError(e);
            }
        });

        Disposable disposable = numbers
            .flatMap(number -> Observable.create(observer -> {
                try {
                    // Simulate some work
                    Thread.sleep(100);
                    observer.onNext(number * 10);
                    observer.onNext(number * 20);
                    observer.onComplete();
                } catch (Exception e) {
                    observer.onError(e);
                }
            }))
            .subscribe(
                item -> System.out.println("FlatMap result: " + item),
                error -> System.out.println("Error: " + error.getMessage()),
                () -> System.out.println("FlatMap completed!")
            );

        // Example 2: Error handling
        System.out.println("\nExample 2: Error handling");
        Observable.create(observer -> {
            try {
                observer.onNext(1);
                observer.onNext(2);
                throw new RuntimeException("Simulated error");
            } catch (Exception e) {
                observer.onError(e);
            }
        })
        .subscribe(
            item -> System.out.println("Received: " + item),
            error -> System.out.println("Error handled: " + error.getMessage()),
            () -> System.out.println("This won't be called due to error")
        );

        // Example 3: Disposable usage
        System.out.println("\nExample 3: Disposable usage");
        Observable<Integer> infinite = Observable.create(observer -> {
            int i = 0;
            while (true) {
                observer.onNext(i++);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    observer.onError(e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        Disposable infiniteDisposable = infinite
            .subscribeOn(ioScheduler)
            .observeOn(computationScheduler)
            .subscribe(
                item -> System.out.println("Infinite: " + item),
                error -> System.out.println("Error: " + error.getMessage()),
                () -> System.out.println("This won't be called")
            );

        // Wait for a while and then dispose
        try {
            Thread.sleep(500);
            infiniteDisposable.dispose();
            System.out.println("Infinite stream disposed");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Wait for all operations to complete
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
} 