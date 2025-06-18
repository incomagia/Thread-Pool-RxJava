package org.example.rx;

/**
 * Interface representing a disposable resource that can be used to cancel a subscription.
 */
public interface Disposable {
    /**
     * Disposes the resource, the operation should be idempotent.
     */
    void dispose();

    /**
     * Returns true if this resource has been disposed.
     * @return true if this resource has been disposed
     */
    boolean isDisposed();
} 