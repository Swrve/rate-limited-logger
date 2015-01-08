package com.swrve.ratelimitedlogger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * An SLF4J-compatible API for rate-limited logging.  Example usage:
 * <p/>
 * <pre>
 *    private static final Logger logger = LoggerFactory.getLogger(getClass());
 *    private static final RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
 *             .maxRate(10).every(10, TimeUnit.SECONDS)
 *             .build();
 * </pre>
 * <p/>
 * This will wrap an existing SLF4J Logger object, allowing a max of 10 messages to be output every 10 seconds,
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
public class RateLimitedLog {
    /**
     * A singleton registry of rate-limited logs, so we can reset them periodically
     */
    static final Registry REGISTRY = new Registry();

    /**
     * We have a limit of 1000 knownPattern objects per RateLimitedLog; exceed this, and it's
     * probable that an already-interpolated string is accidentally being used as a
     * pattern.
     */
    @VisibleForTesting
    static final int MAX_PATTERNS_PER_LOG = 1000;

    private final ConcurrentHashMap<String, RateLimitedLogWithPattern> knownPatterns
            = new ConcurrentHashMap<String, RateLimitedLogWithPattern>();

    private final Logger logger;
    private final RateLimitedLogWithPattern.RateAndPeriod rateAndPeriod;
    private final Registry registry;
    private final Stopwatch stopwatch;
    private final Optional<CounterMetric> stats;

    /**
     * Start building a new RateLimitedLog, wrapping the SLF4J logger @param logger.
     */
    public static RateLimitedLogBuilder.MissingRateAndPeriod withRateLimit(Logger logger) {
        return new RateLimitedLogBuilder.MissingRateAndPeriod(Preconditions.checkNotNull(logger));
    }

    // package-local ctor called by the Builder
    RateLimitedLog(Logger logger, RateLimitedLogWithPattern.RateAndPeriod rateAndPeriod, Stopwatch stopwatch, Optional<CounterMetric> stats, Registry registry) {
        this.logger = logger;
        this.rateAndPeriod = rateAndPeriod;
        this.registry = registry;
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
     * @param template the format string
     * @param args     the list of arguments matching the template
     */
    public void trace(String template, Object... args) {
        get(template).trace(args);
    }

    public void debug(String template, Object... args) {
        get(template).debug(args);
    }

    public void info(String template, Object... args) {
        get(template).info(args);
    }

    public void warn(String template, Object... args) {
        get(template).warn(args);
    }

    public void error(String template, Object... args) {
        get(template).error(args);
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
        RateLimitedLogWithPattern newValue = new RateLimitedLogWithPattern(message, rateAndPeriod, stats, stopwatch, logger);
        RateLimitedLogWithPattern oldValue = knownPatterns.putIfAbsent(message, newValue);
        if (oldValue != null) {
            return oldValue;
        } else {
            // ensure we'll reset the counter once every period
            registry.register(newValue, rateAndPeriod.periodLength);
            return newValue;
        }
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
