package com.swrve.ratelimitedlogger;

import java.util.concurrent.TimeUnit;

/**
 * Simple stopwatch implementation to avoid Guava dependency
 */
public class Stopwatch {

    private long startTime = 0L;

    public Stopwatch() {
        startTime = System.nanoTime();
    }

    public Stopwatch(long startTime) {
        this.startTime = startTime;
    }

    public void start() {
        startTime = System.nanoTime();
    }

    public long elapsedTime(TimeUnit timeUnit) {
        return timeUnit.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }
}
