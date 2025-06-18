package org.example.rx.schedulers;

import org.example.rx.Scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduler that uses a fixed thread pool for computation operations.
 * Similar to RxJava's Schedulers.computation().
 */
public class ComputationScheduler implements Scheduler {
    private final ExecutorService executor;

    public ComputationScheduler() {
        // Use number of available processors for the thread pool size
        int processors = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(processors);
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
} 