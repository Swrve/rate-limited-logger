package com.swrve.ratelimitedlogger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.jcip.annotations.ThreadSafe;
import org.joda.time.Duration;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Internal registry of RateLimitedLogWithPattern objects, allowing periodic resets of their counters.
 */
@ThreadSafe
class Registry {
    private final ConcurrentHashMap<Duration, ConcurrentHashMap<RateLimitedLogWithPattern, Boolean>> registry
            = new ConcurrentHashMap<Duration, ConcurrentHashMap<RateLimitedLogWithPattern, Boolean>>();

    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("RateLimitedLogRegistry-%d")
            .setDaemon(true)
            .build();

    private final ScheduledExecutorService resetScheduler = Executors.newScheduledThreadPool(1, threadFactory);

    /**
     * Register a new @param log, with a reset periodicity of @param period.  This happens relatively infrequently,
     * so synchronization is ok (and safer)
     *
     * @throws IllegalStateException if we run out of space in the registry for that period.
     */
    synchronized void register(RateLimitedLogWithPattern log, Duration period) {

        // if we haven't seen this period before, we'll need to add a schedule to the ScheduledExecutorService
        // to perform a counter reset with that periodicity, otherwise we can count on the existing schedule
        // taking care of it.
        boolean needToScheduleReset = false;

        ConcurrentHashMap<RateLimitedLogWithPattern, Boolean> logLinesForPeriod = registry.get(period);
        if (logLinesForPeriod == null) {
            needToScheduleReset = true;
            logLinesForPeriod = new ConcurrentHashMap<RateLimitedLogWithPattern, Boolean>();
            registry.put(period, logLinesForPeriod);

        } else {
            if (logLinesForPeriod.get(log) != null) {
                return;     // this has already been registered
            }
        }
        logLinesForPeriod.put(log, Boolean.TRUE);

        if (needToScheduleReset) {
            final ConcurrentHashMap<RateLimitedLogWithPattern, Boolean> finalLogLinesForPeriod = logLinesForPeriod;
            resetScheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    resetAllCounters(finalLogLinesForPeriod);
                }
            }, period.getMillis(), period.getMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void resetAllCounters(ConcurrentHashMap<RateLimitedLogWithPattern, Boolean> logLinesForPeriod) {
        for (RateLimitedLogWithPattern log : logLinesForPeriod.keySet()) {
            log.periodicReset();
        }
    }

    synchronized void flush() {
        for (Map.Entry<Duration, ConcurrentHashMap<RateLimitedLogWithPattern, Boolean>>
                entry : registry.entrySet()) {

            ConcurrentHashMap<RateLimitedLogWithPattern, Boolean> logLinesForPeriod = entry.getValue();
            resetAllCounters(logLinesForPeriod);
            logLinesForPeriod.clear();
        }
    }
}
