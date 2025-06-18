package org.example.rx;

/**
 * Interface representing an Observer in the Observer pattern.
 * @param <T> The type of items being observed
 */
public interface Observer<T> {
    /**
     * Called when the Observable emits an item.
     * @param item The item emitted by the Observable
     */
    void onNext(T item);

    /**
     * Called when the Observable encounters an error.
     * @param t The error that occurred
     */
    void onError(Throwable t);

    /**
     * Called when the Observable has completed emitting items.
     */
    void onComplete();
} 