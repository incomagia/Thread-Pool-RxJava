package org.example.rx;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class representing an Observable in the Observer pattern.
 * @param <T> The type of items being emitted
 */
public class Observable<T> {
    private final Consumer<Observer<T>> source;

    private Observable(Consumer<Observer<T>> source) {
        this.source = source;
    }

    /**
     * Creates a new Observable from a source function.
     * @param source The function that defines how the Observable emits items
     * @param <T> The type of items being emitted
     * @return A new Observable instance
     */
    public static <T> Observable<T> create(Consumer<Observer<T>> source) {
        return new Observable<>(source);
    }

    /**
     * Subscribes an Observer to this Observable and returns a Disposable.
     * @param observer The Observer to subscribe
     * @return A Disposable that can be used to cancel the subscription
     */
    public Disposable subscribe(Observer<T> observer) {
        AtomicBoolean disposed = new AtomicBoolean(false);
        try {
            source.accept(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    if (!disposed.get()) {
                        observer.onNext(item);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (!disposed.get()) {
                        observer.onError(t);
                    }
                }

                @Override
                public void onComplete() {
                    if (!disposed.get()) {
                        observer.onComplete();
                    }
                }
            });
        } catch (Exception e) {
            if (!disposed.get()) {
                observer.onError(e);
            }
        }
        return new Disposable() {
            @Override
            public void dispose() {
                disposed.set(true);
            }

            @Override
            public boolean isDisposed() {
                return disposed.get();
            }
        };
    }

    /**
     * Subscribes to this Observable with callbacks for onNext, onError, and onComplete.
     * @param onNext The callback for handling emitted items
     * @param onError The callback for handling errors
     * @param onComplete The callback for handling completion
     * @return A Disposable that can be used to cancel the subscription
     */
    public Disposable subscribe(Consumer<T> onNext, Consumer<Throwable> onError, Runnable onComplete) {
        return subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                onNext.accept(item);
            }

            @Override
            public void onError(Throwable t) {
                onError.accept(t);
            }

            @Override
            public void onComplete() {
                onComplete.run();
            }
        });
    }

    /**
     * Transforms the items emitted by this Observable by applying a function to each item.
     * @param mapper The function to apply to each item
     * @param <R> The type of items emitted by the resulting Observable
     * @return A new Observable that emits the transformed items
     */
    public <R> Observable<R> map(Function<T, R> mapper) {
        return new Observable<>(observer -> subscribe(
            item -> {
                try {
                    observer.onNext(mapper.apply(item));
                } catch (Exception e) {
                    observer.onError(e);
                }
            },
            observer::onError,
            observer::onComplete
        ));
    }

    /**
     * Filters items emitted by this Observable by only emitting those that satisfy a predicate.
     * @param predicate The predicate to apply to each item
     * @return A new Observable that emits only those items that satisfy the predicate
     */
    public Observable<T> filter(Predicate<T> predicate) {
        return new Observable<>(observer -> subscribe(
            item -> {
                try {
                    if (predicate.test(item)) {
                        observer.onNext(item);
                    }
                } catch (Exception e) {
                    observer.onError(e);
                }
            },
            observer::onError,
            observer::onComplete
        ));
    }

    /**
     * Transforms the items emitted by this Observable into Observables, then flattens the emissions from those into a single Observable.
     * @param mapper A function that returns an Observable for each item emitted by the source Observable
     * @param <R> The type of items emitted by the resulting Observable
     * @return A new Observable that emits the items emitted by the Observables returned by the mapper function
     */
    public <R> Observable<R> flatMap(Function<T, Observable<R>> mapper) {
        return new Observable<>(observer -> {
            AtomicBoolean disposed = new AtomicBoolean(false);
            subscribe(
                item -> {
                    if (!disposed.get()) {
                        try {
                            Observable<R> innerObservable = mapper.apply(item);
                            innerObservable.subscribe(
                                observer::onNext,
                                observer::onError,
                                () -> {} // Don't complete when inner completes
                            );
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                },
                observer::onError,
                observer::onComplete
            );
        });
    }

    /**
     * Specifies the Scheduler on which an Observable will operate.
     * @param scheduler The Scheduler to use
     * @return A new Observable that operates on the specified Scheduler
     */
    public Observable<T> subscribeOn(Scheduler scheduler) {
        return new Observable<>(observer -> scheduler.execute(() -> subscribe(observer)));
    }

    /**
     * Specifies the Scheduler on which an Observer will observe this Observable.
     * @param scheduler The Scheduler to use
     * @return A new Observable that is observed on the specified Scheduler
     */
    public Observable<T> observeOn(Scheduler scheduler) {
        return new Observable<>(observer -> subscribe(
            item -> scheduler.execute(() -> observer.onNext(item)),
            error -> scheduler.execute(() -> observer.onError(error)),
            () -> scheduler.execute(observer::onComplete)
        ));
    }
} 