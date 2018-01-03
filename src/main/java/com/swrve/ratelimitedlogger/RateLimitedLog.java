package com.swrve.ratelimitedlogger;

import org.slf4j.Logger;
import org.slf4j.Marker;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An SLF4J-compatible API for rate-limited logging.  Example usage:
 * <p/>
 * <pre>
 *    private static final Logger logger = LoggerFactory.getLogger(getClass());
 *    private static final RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
 *             .maxRate(5).every(Duration.ofSeconds(10))
 *             .build();
 * </pre>
 * <p/>
 * This will wrap an existing SLF4J Logger object, allowing a max of 5 messages to be output every 10 seconds,
 * suppressing any more than that.  When a log is suppressed, at the end of the 10-second period, another
 * log message is output indicating how many logs were hidden.  This style of rate limiting is the same as the
 * one used by UNIX syslog, so should be comprehensible, easy to predict, and familiar to many users, unlike
 * more complex adaptive rate limits.
 * <p/>
 * Each log message has its own internal rate limiting counter.  In other words, if you have 2 log messages, you can
 * safely reuse the same RateLimitedLog object to log both, and the rate of one will not caused the other to
 * be suppressed as a side effect. However, this means that if you wish to include dynamic, variable data in the log
 * output, you will need to use SLF4J-style templates, instead of ("foo " + bar + " baz") string interpolation.
 * For example:
 * <p/>
 * <pre>
 *    rateLimitedLog.info("Just saw an event of type {}: {}", event.getType(), event);
 * </pre>
 * <p/>
 * "{}" will implicitly invoke an object's toString() method, so toString() does not need
 * to be called explicitly when logging.  (This has obvious performance benefits, in that
 * those toString() methods will not be called at all once the rate limits have been exceeded.)
 * <p/>
 * Where performance is critical, note that you can obtain a reference to the RateLimitedLogWithPattern object
 * for an individual log template, which will avoid a ConcurrentHashMap lookup.
 * <p/>
 * The RateLimitedLog objects are thread-safe.
 */
@ThreadSafe
public class RateLimitedLog implements Logger {
    /**
     * A singleton registry of rate-limited logs, so we can reset them periodically
     */
    static final Registry REGISTRY = new Registry();

    /**
     * We have a limit of 1000 knownPattern objects per RateLimitedLog; exceed this, and it's
     * probable that an already-interpolated string is accidentally being used as a
     * pattern.
     */
    static final int MAX_PATTERNS_PER_LOG = 1000;

    private final ConcurrentHashMap<String, RateLimitedLogWithPattern> knownPatterns
            = new ConcurrentHashMap<>();

    private final Logger logger;
    private final RateLimitedLogWithPattern.RateAndPeriod rateAndPeriod;
    private final Registry registry;
    private final Stopwatch stopwatch;
    private final CounterMetric stats;

    /**
     * Start building a new RateLimitedLog, wrapping the SLF4J logger @param logger.
     */
    public static RateLimitedLogBuilder.MissingRateAndPeriod withRateLimit(Logger logger) {
        return new RateLimitedLogBuilder.MissingRateAndPeriod(Objects.requireNonNull(logger));
    }

    // package-local ctor called by the Builder
    RateLimitedLog(Logger logger, RateLimitedLogWithPattern.RateAndPeriod rateAndPeriod, Stopwatch stopwatch, CounterMetric stats, Registry registry) {
        this.logger = logger;
        this.rateAndPeriod = rateAndPeriod;
        this.registry = registry;
        this.stats = stats;
        this.stopwatch = stopwatch;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        get(msg).trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        get(format).trace(arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        get(format).trace(arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        get(format).trace(arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        get(msg).trace(t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        get(msg).trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        get(format).trace(marker, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        get(format).trace(marker, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        get(format).trace(marker, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        get(msg).trace(marker, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        get(msg).debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        get(format).debug(arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        get(format).debug(arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        get(format).debug(arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        get(msg).debug(t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        get(msg).debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        get(format).debug(marker, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        get(format).debug(marker, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... argArray) {
        get(format).debug(marker, argArray);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        get(msg).debug(marker, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        get(msg).info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        get(format).info(arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        get(format).info(arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        get(format).info(arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        get(msg).info(t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        get(msg).info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        get(format).info(marker, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        get(format).info(marker, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... argArray) {
        get(format).info(marker, argArray);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        get(msg).info(marker, t);
    }
    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        get(msg).warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        get(format).warn(arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        get(format).warn(arg1, arg2);
    }

    @Override
    public void warn(String format, Object... arguments) {
        get(format).warn(arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        get(msg).warn(t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        get(msg).warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        get(format).warn(marker, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        get(format).warn(marker, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... argArray) {
        get(format).warn(marker, argArray);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        get(msg).warn(marker, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        get(msg).error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        get(format).error(arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        get(format).error(arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        get(format).error(arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        get(msg).error(t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        get(msg).error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        get(format).error(marker, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        get(format).error(marker, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... argArray) {
        get(format).error(marker, argArray);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        get(msg).error(marker, t);
    }

    /**
     * @return a RateLimitedLogWithPattern object for the supplied @param message.  This can be cached and
     * reused by callers in performance-sensitive cases to avoid performing a ConcurrentHashMap lookup.
     * <p/>
     * Note that the string is the sole key used, so the same string cannot be reused with differing period
     * settings; any periods which differ from the first one used are ignored.
     * <p/>
     * @throws IllegalStateException if we exceed the limit on number of RateLimitedLogWithPattern objects
     * in any one period; if this happens, it's probable that an already-interpolated string is
     * accidentally being used as a log pattern.
     */
    public RateLimitedLogWithPattern get(final String message) {
        // fast path: hopefully we can do this without creating a Supplier object
        RateLimitedLogWithPattern got = knownPatterns.get(message);
        if (got != null) {
            return got;
        }

        // before we create another one, check cache capacity first
        if (knownPatterns.size() > MAX_PATTERNS_PER_LOG) {
            outOfCacheCapacity();
        }

        // slow path: create a RateLimitedLogWithPattern
        RateLimitedLogWithPattern newValue = new RateLimitedLogWithPattern(message, rateAndPeriod, registry, stats, stopwatch, logger);
        RateLimitedLogWithPattern oldValue = knownPatterns.putIfAbsent(message, newValue);
        if (oldValue != null) {
            return oldValue;
        } else {
            return newValue;
        }
    }

    /**
     * @return a LogWithPatternAndLevel object for the supplied @param message and
     * @param level .  This can be cached and reused by callers in performance-sensitive
     * cases to avoid performing two ConcurrentHashMap lookups.
     * <p/>
     * Note that the string is the sole key used, so the same string cannot be reused with differing period
     * settings; any periods which differ from the first one used are ignored.
     * <p/>
     * @throws IllegalStateException if we exceed the limit on number of RateLimitedLogWithPattern objects
     * in any one period; if this happens, it's probable that an already-interpolated string is
     * accidentally being used as a log pattern.
     */
    public LogWithPatternAndLevel get(String pattern, Level level) {
        return get(pattern).get(level);
    }

    /**
     * We've run out of capacity in our cache of RateLimitedLogWithPattern objects.  This probably
     * means that the caller is accidentally calling us with an already-interpolated string, instead
     * of using the pattern as the key and letting us do the interpolation.  Don't lose data;
     * instead, fall back to flushing the entire cache but carrying on.  The worst-case scenario
     * here is that we flush the logs far more frequently than their requested durations, potentially
     * allowing the logging to impact throughput, but we don't lose any log data.
     */
    private void outOfCacheCapacity() {
        synchronized (knownPatterns) {
            if (knownPatterns.size() > MAX_PATTERNS_PER_LOG) {
                logger.warn("out of capacity in RateLimitedLog registry; accidentally " +
                        "using interpolated strings as patterns?");
                registry.flush();
                knownPatterns.clear();
            }
        }
    }
}
