package com.swrve.ratelimitedlogger;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.joda.time.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An individual log pattern - the unit of rate limiting.  Each object is rate-limited individually.
 * <p/>
 * These objects are thread-safe.
 */
@ThreadSafe
public class RateLimitedLogWithPattern {
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
     * When we exceed the rate limit during a period, we record when, and with what message, it occurred for the
     * subsequent "(suppressed N messages)" log.
     */
    @GuardedBy("this")
    private Optional<String> rateLimitLine = Optional.absent(); // mutable
    @GuardedBy("this")
    private Optional<Long> rateLimitedAt = Optional.absent(); // mutable

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
        if (!isRateLimited(args)) {
            logger.trace(message, args);
        }
        incrementStats("trace");
    }

    public void debug(Object... args) {
        if (!isRateLimited(args)) {
            logger.debug(message, args);
        }
        incrementStats("debug");
    }

    public void info(Object... args) {
        if (!isRateLimited(args)) {
            logger.info(message, args);
        }
        incrementStats("info");
    }

    public void warn(Object... args) {
        if (!isRateLimited(args)) {
            logger.warn(message, args);
        }
        incrementStats("warn");
    }

    public void error(Object... args) {
        if (!isRateLimited(args)) {
            logger.error(message, args);
        }
        incrementStats("error");
    }

    private boolean isRateLimited(Object... args) {
        int count = counter.incrementAndGet();
        if (count < rateAndPeriod.maxRate) {
            return false;
        } else if (count == rateAndPeriod.maxRate) {
            haveJustExceededRateLimit();
            return false;
        } else {
            return true;
        }
    }

    /**
     * Reset the counter and suppression details, if necessary.  This is called once every period, by the Registry.
     */
    synchronized void periodicReset() {
        int count = counter.get();
        counter.addAndGet(-count);

        if (count > rateAndPeriod.maxRate) {
            Preconditions.checkState(rateLimitLine.isPresent());
            Preconditions.checkState(rateLimitedAt.isPresent());
            int numSuppressed = count - rateAndPeriod.maxRate;
            Duration howLong = new Duration(rateLimitedAt.get(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
            logger.info("(suppressed " + numSuppressed + " logs similar to '" + rateLimitLine.get() + "' in " + howLong + ")");
        }

        rateLimitLine = Optional.absent();
        rateLimitedAt = Optional.absent();
    }

    private synchronized void haveJustExceededRateLimit() {
        // record the last line we log, so we have something to say the rate-limited
        // lines were similar to
        rateLimitLine = Optional.of(message);
        rateLimitedAt = Optional.of(stopwatch.elapsed(TimeUnit.MILLISECONDS));
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
    public boolean equals(Object o) {
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
