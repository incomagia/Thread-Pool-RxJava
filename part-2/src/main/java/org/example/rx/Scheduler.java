package org.example.rx;

/**
 * Interface for scheduling tasks to be executed on different threads.
 */
public interface Scheduler {
    /**
     * Schedules a task for execution.
     * @param task The task to be executed
     */
    void execute(Runnable task);
} 