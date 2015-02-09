package com.swrve.ratelimitedlogger;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.joda.time.Duration;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An individual log pattern - the unit of rate limiting.  Each object is rate-limited individually.
 * <p/>
 * These objects are thread-safe.
 */
@ThreadSafe
public class RateLimitedLogWithPattern {
    private static final long NOT_RATE_LIMITED_YET = 0L;

    private final String message;
    private final RateAndPeriod rateAndPeriod;
    private final Logger logger;
    private final Optional<CounterMetric> stats;
    private final Stopwatch stopwatch;

    /**
     * Number of observed logs in the current time period.
     */
    private final AtomicInteger counter = new AtomicInteger(0); // mutable

    /**
     * When we exceed the rate limit during a period, we record when.  If the rate limit has not been exceeded, the
     * magic value of NOT_RATE_LIMITED_YET will be recorded.
     */
    private final AtomicLong rateLimitedAt = new AtomicLong(NOT_RATE_LIMITED_YET); // mutable

    RateLimitedLogWithPattern(String message, RateAndPeriod rateAndPeriod, Optional<CounterMetric> stats, Stopwatch stopwatch, Logger logger) {
        this.message = message;
        this.rateAndPeriod = rateAndPeriod;
        this.logger = logger;
        this.stats = stats;
        this.stopwatch = stopwatch;
    }

    /**
     * logging APIs.
     * <p/>
     * These can use the SLF4J style of templating to parameterize the Logs.
     * See http://www.slf4j.org/api/org/slf4j/helpers/MessageFormatter.html .
     * <p/>
     * <pre>
     *    rateLimitedLog.info("Just saw an event of type {}: {}", event.getType(), event);
     * </pre>
     *
     * @param args the varargs list of arguments matching the message template
     */
    public void trace(Object... args) {
        if (!isRateLimited()) {
            logger.trace(message, args);
        }
        incrementStats("trace");
    }

    public void debug(Object... args) {
        if (!isRateLimited()) {
            logger.debug(message, args);
        }
        incrementStats("debug");
    }

    public void info(Object... args) {
        if (!isRateLimited()) {
            logger.info(message, args);
        }
        incrementStats("info");
    }

    public void warn(Object... args) {
        if (!isRateLimited()) {
            logger.warn(message, args);
        }
        incrementStats("warn");
    }

    public void error(Object... args) {
        if (!isRateLimited()) {
            logger.error(message, args);
        }
        incrementStats("error");
    }

    private boolean isRateLimited() {

        // note: this method is not synchronized, for performance.  If we exceed the maxRate, we will start checking
        // haveExceededLimit, and if that's still false, we enter the synchronized haveJustExceededRateLimit() method.
        //
        // There is still potential for a race -- the rate of incrementing could be so high that we are already
        // over the maxRate by the time the reset thread runs, but the haveJustExceededRateLimit() hasn't yet been
        // run in this thread. In this scenario, we will fail to notice that we are over the limit, but when
        // the next iteration runs, we will correctly report the correct number of suppressions and the time
        // when haveJustExceededRateLimit() eventually got to execute.  We will also potentially log a small
        // number more lines to the logger than the rate limit allows.
        //
        int count = counter.incrementAndGet();
        if (count < rateAndPeriod.maxRate) {
            return false;
        } else if (count >= rateAndPeriod.maxRate && rateLimitedAt.get() == NOT_RATE_LIMITED_YET) {
            haveJustExceededRateLimit();
            return false; // we still issue this final log, though
        } else {
            return true;
        }
    }

    /**
     * Reset the counter and suppression details, if necessary.  This is called once every period, by the Registry.
     */
    synchronized void periodicReset() {
        long whenLimited = rateLimitedAt.getAndSet(NOT_RATE_LIMITED_YET);
        if (whenLimited != NOT_RATE_LIMITED_YET) {
            reportSuppression(whenLimited);
        }
    }

    @GuardedBy("this")
    private void reportSuppression(long whenLimited) {
        int count = counter.get();
        counter.addAndGet(-count);
        int numSuppressed = count - rateAndPeriod.maxRate;
        if (numSuppressed == 0) {
            return;  // special case: we hit the rate limit, but did not actually exceed it -- nothing got suppressed, so there's no need to log
        }
        Duration howLong = new Duration(whenLimited, elapsedMsecs());
        logger.info("(suppressed " + numSuppressed + " logs similar to '" + message + "' in " + howLong + ")");
    }

    private synchronized void haveJustExceededRateLimit() {
        rateLimitedAt.set(elapsedMsecs());
    }

    private long elapsedMsecs() {
        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        if (elapsed == NOT_RATE_LIMITED_YET) {
            elapsed++;  // avoid using the magic value by "rounding up"
        }
        return elapsed;
    }

    /**
     * Increment a counter metric called "{level}_rate_limited_log_count", where "{level}" is the log
     * level in question.  This is still performed even when a log is rate limited, since incrementing
     * a counter metric is cheap!
     * <p/>
     * This deliberately doesn't attempt to use counter metrics named after the log message, since
     * extracting that without making a mess is complex, and if that's desired, it's easy enough
     * for calling code to do it instead.  As an "early warning" indicator that lots of logging
     * activity took place, this is useful enough.
     */
    private void incrementStats(String level) {
        if (!stats.isPresent()) {
            return;
        }
        stats.get().increment(level + "_rate_limited_log_count");
    }

    /**
     * Two RateLimitedLogWithPattern objects are considered equal if their messages match; the
     * RateAndPeriods are not significant.
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return (message.equals(((RateLimitedLogWithPattern) o).message));
    }

    @Override
    public int hashCode() {
        return message.hashCode();
    }


    static final class RateAndPeriod {
        final int maxRate;
        final Duration periodLength;

        public RateAndPeriod(int maxRate, Duration periodLength) {
            this.maxRate = maxRate;
            this.periodLength = periodLength;
        }
    }
}
