package com.swrve.ratelimitedlogger;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import net.jcip.annotations.ThreadSafe;
import org.joda.time.Duration;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * An individual log pattern.  Each object is rate-limited individually but with separation on the log level.
 * <p/>
 * These objects are thread-safe.
 */
@ThreadSafe
public class RateLimitedLogWithPattern {

    private final String message;
    private final RateAndPeriod rateAndPeriod;
    private final Logger logger;
    private final Registry registry;
    private final Optional<CounterMetric> stats;
    private final Stopwatch stopwatch;
    private final AtomicReferenceArray<LogWithPatternAndLevel> levels;

    RateLimitedLogWithPattern(String message, RateAndPeriod rateAndPeriod, Registry registry, Optional<CounterMetric> stats, Stopwatch stopwatch, Logger logger) {
        this.message = message;
        this.rateAndPeriod = rateAndPeriod;
        this.registry = registry;
        this.logger = logger;
        this.stats = stats;
        this.stopwatch = stopwatch;
        this.levels = new AtomicReferenceArray<LogWithPatternAndLevel>(Level.values().length);
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
        get(Level.TRACE).log(args);
    }

    public void debug(Object... args) {
        get(Level.DEBUG).log(args);
    }

    public void info(Object... args) {
        get(Level.INFO).log(args);
    }

    public void warn(Object... args) {
        get(Level.WARN).log(args);
    }

    public void error(Object... args) {
        get(Level.ERROR).log(args);
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
        return (message.equals(((RateLimitedLogWithPattern) o).message));
    }

    @Override
    public int hashCode() {
        return message.hashCode();
    }

    /**
     * @return a LogWithPatternAndLevel object for the supplied @param level .
     * This can be cached and reused by callers in performance-sensitive
     * cases to avoid performing two ConcurrentHashMap lookups.
     * <p/>
     * Note that the string is the sole key used, so the same string cannot be reused with differing period
     * settings; any periods which differ from the first one used are ignored.
     * <p/>
     * @throws IllegalStateException if we exceed the limit on number of RateLimitedLogWithPattern objects
     * in any one period; if this happens, it's probable that an already-interpolated string is
     * accidentally being used as a log pattern.
     */
    public LogWithPatternAndLevel get(Level level) {
        int l = level.ordinal();

        LogWithPatternAndLevel got = levels.get(l);
        if (got != null) {
            return got;
        }

        // slow path: create a new LogWithPatternAndLevel
        LogWithPatternAndLevel newValue = new LogWithPatternAndLevel(message,
                level, rateAndPeriod, stats, stopwatch, logger);

        boolean wasSet = levels.compareAndSet(l, null, newValue);
        if (!wasSet) {
            return Preconditions.checkNotNull(levels.get(l));
        } else {
            // ensure we'll reset the counter once every period
            registry.register(newValue, rateAndPeriod.periodLength);
            return newValue;
        }
    }

    public static final class RateAndPeriod {
        final int maxRate;
        final Duration periodLength;

        public RateAndPeriod(int maxRate, Duration periodLength) {
            this.maxRate = maxRate;
            this.periodLength = periodLength;
        }
    }
}
