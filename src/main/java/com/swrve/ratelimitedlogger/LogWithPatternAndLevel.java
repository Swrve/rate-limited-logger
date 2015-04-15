package com.swrve.ratelimitedlogger;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.joda.time.Duration;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An individual log pattern and level - the unit of rate limiting.  Each object is rate-limited
 * individually.
 * <p/>
 * Thread-safe.
 */
@ThreadSafe
public class LogWithPatternAndLevel {

    private static final long NOT_RATE_LIMITED_YET = 0L;
    private static final String RATE_LIMITED_COUNT_SUFFIX = "_rate_limited_log_count";

    private final String message;
    private final Level level;
    private final RateLimitedLogWithPattern.RateAndPeriod rateAndPeriod;
    private final Logger logger;
    private final Optional<CounterMetric> stats;
    private final Stopwatch stopwatch;

    /**
     * Number of observed logs in the current time period based on the log level.
     */
    private final AtomicLong counter = new AtomicLong(0L); // mutable

    /**
     * When we exceed the rate limit during a period, we record when.  If the rate limit has not been exceeded, the
     * magic value of NOT_RATE_LIMITED_YET will be recorded.
     */
    private final AtomicLong rateLimitedAt = new AtomicLong(NOT_RATE_LIMITED_YET); // mutable

    LogWithPatternAndLevel(String message, Level level,
                           RateLimitedLogWithPattern.RateAndPeriod rateAndPeriod,
                           Optional<CounterMetric> stats,
                           Stopwatch stopwatch, Logger logger) {
        this.message = message;
        this.level = level;
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
    public void log(Object... args) {
        if (!isRateLimited()) {
            LogLevelHelper.log(logger, level, message, args);
        }
        incrementStats();
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
        long count = counter.incrementAndGet();
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
        long count = counter.get();
        counter.addAndGet(-count);
        long numSuppressed = count - rateAndPeriod.maxRate;
        if (numSuppressed == 0) {
            return;  // special case: we hit the rate limit, but did not actually exceed it -- nothing got suppressed, so there's no need to log
        }
        Duration howLong = new Duration(whenLimited, elapsedMsecs());
        LogLevelHelper.log(logger, level, "(suppressed {} logs similar to '{}' in {})", numSuppressed, message, howLong);
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
    private void incrementStats() {
        if (!stats.isPresent()) {
            return;
        }
        stats.get().increment(level.getLevelName() + RATE_LIMITED_COUNT_SUFFIX);
    }

    /**
     * Two RateLimitedLogWithPattern objects are considered equal if their messages match; the
     * RateAndPeriods are not significant.
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogWithPatternAndLevel other = (LogWithPatternAndLevel) o;
        if (other.level != level) {
            return false;
        }
        return (message.equals(other.message));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(message, level.ordinal());
    }
}
