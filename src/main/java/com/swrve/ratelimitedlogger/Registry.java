package com.swrve.ratelimitedlogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Internal registry of LogWithPatternAndLevel objects, allowing periodic resets of their counters.
 */
@ThreadSafe
class Registry {
    private static final Logger logger = LoggerFactory.getLogger(Registry.class);

    private final ConcurrentHashMap<Duration, ConcurrentHashMap<LogWithPatternAndLevel, Boolean>> registry
            = new ConcurrentHashMap<>();

    private final ThreadFactory threadFactory = new ThreadFactory() {
        final AtomicLong count = new AtomicLong(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(String.format(Locale.ROOT, "RateLimitedLogRegistry-%d", count.getAndIncrement()));
            thread.setDaemon(true);
            return thread;
        }
    };

    private final ScheduledExecutorService resetScheduler = Executors.newScheduledThreadPool(1, threadFactory);

    Registry() {
        // this will ensure that we will always flush any suppressed logs prior to exiting a process
        Runtime.getRuntime().addShutdownHook(new Thread(this::flush));
    }

    /**
     * Register a new @param log, with a reset periodicity of @param period.  This happens relatively infrequently,
     * so synchronization is ok (and safer)
     *
     * @throws IllegalStateException if we run out of space in the registry for that period.
     */
    synchronized void register(LogWithPatternAndLevel log, Duration period) {

        // if we haven't seen this period before, we'll need to add a schedule to the ScheduledExecutorService
        // to perform a counter reset with that periodicity, otherwise we can count on the existing schedule
        // taking care of it.
        boolean needToScheduleReset = false;

        ConcurrentHashMap<LogWithPatternAndLevel, Boolean> logLinesForPeriod = registry.get(period);
        if (logLinesForPeriod == null) {
            needToScheduleReset = true;
            logLinesForPeriod = new ConcurrentHashMap<>();
            registry.put(period, logLinesForPeriod);

        } else {
            if (logLinesForPeriod.get(log) != null) {
                return;     // this has already been registered
            }
        }
        logLinesForPeriod.put(log, Boolean.TRUE);

        if (needToScheduleReset) {
            final ConcurrentHashMap<LogWithPatternAndLevel, Boolean> finalLogLinesForPeriod = logLinesForPeriod;
            resetScheduler.scheduleWithFixedDelay(() -> {
                try {
                    resetAllCounters(finalLogLinesForPeriod);
                } catch (Exception e) {
                    logger.warn("failed to reset counters: " + e, e);
                    // but carry on in the next iteration
                }
            }, period.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void resetAllCounters(ConcurrentHashMap<LogWithPatternAndLevel, Boolean> logLinesForPeriod) {
        for (LogWithPatternAndLevel log : logLinesForPeriod.keySet()) {
            log.periodicReset();
        }
    }

    synchronized void flush() {
        for (Map.Entry<Duration, ConcurrentHashMap<LogWithPatternAndLevel, Boolean>>
                entry : registry.entrySet()) {

            ConcurrentHashMap<LogWithPatternAndLevel, Boolean> logLinesForPeriod = entry.getValue();
            resetAllCounters(logLinesForPeriod);
            logLinesForPeriod.clear();
        }
    }
}
