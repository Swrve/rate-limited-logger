package com.swrve.ratelimitedlogger;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import org.joda.time.Duration;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class RateLimitedLogTest {

    @Test
    public void basic() {
        MockLogger logger = new MockLogger();
        final AtomicLong mockTime = new AtomicLong(0L);

        RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(10))
                .withStopwatch(createStopwatch(mockTime))
                .build();

        assertThat(logger.infoMessageCount, equalTo(0));

        mockTime.set(1L);
        rateLimitedLog.info("basic");
        mockTime.set(2L);

        assertThat(logger.infoMessageCount, equalTo(1));
    }

    @Test
    public void trace() {
        MockLogger logger = new MockLogger();
        final AtomicLong mockTime = new AtomicLong(0L);

        RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(10))
                .withStopwatch(createStopwatch(mockTime))
                .build();

        rateLimitedLog.trace("trace");
        assertThat(logger.traceMessageCount, equalTo(1));
    }

    @Test
    public void rateLimitingWorks() throws InterruptedException {
        MockLogger logger = new MockLogger();
        final AtomicLong mockTime = new AtomicLong(0L);

        RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(500))
                .withStopwatch(createStopwatch(mockTime))
                .build();

        assertThat(logger.infoMessageCount, equalTo(0));

        mockTime.set(1L);
        rateLimitedLog.info("rateLimited {}", 1);
        mockTime.set(2L);
        rateLimitedLog.info("rateLimited {}", 2);

        // the second message should have been suppressed
        assertThat(logger.infoMessageCount, equalTo(1));

        Thread.sleep(600L);
        mockTime.set(601L);

        // zeroing the counter should produce a "similar messages suppressed" message
        assertThat(logger.infoMessageCount, equalTo(2));

        Thread.sleep(500L);
        mockTime.set(1101L);

        // no logs in the meantime, so no new message
        assertThat(logger.infoMessageCount, equalTo(2));
    }

    @Test
    public void rateLimitingWorksTwice() throws InterruptedException {
        MockLogger logger = new MockLogger();
        final AtomicLong mockTime = new AtomicLong(0L);

        RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(500))
                .withStopwatch(createStopwatch(mockTime))
                .build();

        assertThat(logger.infoMessageCount, equalTo(0));

        mockTime.set(1L);
        rateLimitedLog.info("rateLimitingWorks2 {}", 1);
        mockTime.set(2L);
        rateLimitedLog.info("rateLimitingWorks2 {}", 2);

        // the second message should have been suppressed
        assertThat(logger.infoMessageCount, equalTo(1));

        Thread.sleep(1000L);
        mockTime.set(601L);

        // by now, we should have seen the "similar messages suppressed" message
        assertThat(logger.infoMessageCount, equalTo(2));

        rateLimitedLog.info("rateLimitingWorks2 {}", 3);
        rateLimitedLog.info("rateLimitingWorks2 {}", 4);
        rateLimitedLog.info("rateLimitingWorks2 {}", 5);

        // should have suppressed 4 and 5
        assertThat(logger.infoMessageCount, equalTo(3));

        Thread.sleep(500L);
        mockTime.set(1101L);

        // should have seen a second "suppressed" message after 500ms
        assertThat(logger.infoMessageCount, equalTo(4));
    }

    @Test
    public void rateLimitingNonZeroRateAndAllThresholds() throws InterruptedException {
        MockLogger logger = new MockLogger();
        final AtomicLong mockTime = new AtomicLong(0L);

        RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(3).every(Duration.millis(500))
                .withStopwatch(createStopwatch(mockTime))
                .build();

        assertThat(logger.infoMessageCount, equalTo(0));

        mockTime.set(1L);
        rateLimitedLog.info("rateLimitingNonZeroRateAndAllThresholds {}", 1);
        rateLimitedLog.info("rateLimitingNonZeroRateAndAllThresholds {}", 1);
        rateLimitedLog.info("rateLimitingNonZeroRateAndAllThresholds {}", 1);
        rateLimitedLog.info("rateLimitingNonZeroRateAndAllThresholds {}", 1);
        mockTime.set(2L);
        rateLimitedLog.debug("rateLimitingNonZeroRateAndAllThresholds {}", 2);
        mockTime.set(498L);
        rateLimitedLog.warn("rateLimitingNonZeroRateAndAllThresholds {}", 3);
        mockTime.set(499L);
        rateLimitedLog.trace("rateLimitingNonZeroRateAndAllThresholds {}", 5);

        assertThat(logger.infoMessageCount, equalTo(3));
        assertThat(logger.debugMessageCount, equalTo(1));
        assertThat(logger.warnMessageCount, equalTo(1));
        assertThat(logger.errorMessageCount, equalTo(0));
        assertThat(logger.traceMessageCount, equalTo(1));

        Thread.sleep(600L);
        mockTime.set(600L);

        // zeroing the counter should produce a "similar messages suppressed" message
        assertThat(logger.infoMessageCount, equalTo(4));

        // and now we should be able to log a message
        rateLimitedLog.error("rateLimitingNonZeroRateAndAllThresholds {0}", 1);
        assertThat(logger.errorMessageCount, equalTo(1));
    }

    @Test
    public void multiplePeriods() throws InterruptedException {
        MockLogger logger = new MockLogger();
        final AtomicLong mockTime = new AtomicLong(0L);

        RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(200))
                .withStopwatch(createStopwatch(mockTime))
                .build();

        RateLimitedLog rateLimitedLog2 = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(300))
                .withStopwatch(createStopwatch(mockTime))
                .build();

        assertThat(logger.infoMessageCount, equalTo(0));

        mockTime.set(1L);
        rateLimitedLog.info("multiplePeriods {}", 1);
        mockTime.set(2L);
        rateLimitedLog.info("multiplePeriods {}", 2);
        mockTime.set(3L);
        rateLimitedLog2.info("multiplePeriods2 {}", 1);
        mockTime.set(4L);
        rateLimitedLog2.info("multiplePeriods2 {}", 2);

        // the second message should have been suppressed
        assertThat(logger.infoMessageCount, equalTo(2));

        Thread.sleep(250L);
        mockTime.set(251L);
        assertThat(logger.infoMessageCount, equalTo(3));

        Thread.sleep(100L);
        mockTime.set(351L);
        assertThat(logger.infoMessageCount, equalTo(4));
    }

    @Test
    public void withMetrics() {
        MockLogger logger = new MockLogger();
        final AtomicLong mockTime = new AtomicLong(0L);
        final AtomicBoolean statsCalled = new AtomicBoolean(false);

        CounterMetric mockStats = new CounterMetric() {
            @Override
            public void increment(String name) {
                statsCalled.set(true);
            }
        };
        RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(10))
                .withStopwatch(createStopwatch(mockTime))
                .recordMetrics(mockStats)
                .build();

        assertThat(logger.infoMessageCount, equalTo(0));

        mockTime.set(1L);
        rateLimitedLog.info("withMetrics");
        mockTime.set(2L);

        assertThat(logger.infoMessageCount, equalTo(1));
        assertThat(statsCalled.get(), equalTo(true));
    }

    @Test
    public void testGet() {
        MockLogger logger = new MockLogger();
        final AtomicLong mockTime = new AtomicLong(0L);

        RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(10))
                .withStopwatch(createStopwatch(mockTime))
                .build();

        RateLimitedLogWithPattern line = rateLimitedLog.get("testGet");

        assertThat(logger.infoMessageCount, equalTo(0));

        mockTime.set(1L);
        line.info();

        assertThat(logger.infoMessageCount, equalTo(1));

        RateLimitedLogWithPattern line2 = rateLimitedLog.get("testGet");
        assertThat(line2, equalTo(line));
        assertThat(line2, not(equalTo(null)));

        RateLimitedLogWithPattern line3 = rateLimitedLog.get("testGet2");
        assertThat(line3, not(equalTo(line)));
        assertThat(line3, not(equalTo(null)));

        assertThat(line2.hashCode(), equalTo(line.hashCode()));
        assertThat(line2.hashCode(), not(equalTo(line3.hashCode())));
    }

    @Test
    public void testGetWithLevel() {
        MockLogger logger = new MockLogger();
        final AtomicLong mockTime = new AtomicLong(0L);

        RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(10))
                .withStopwatch(createStopwatch(mockTime))
                .build();

        LogWithPatternAndLevel line = rateLimitedLog.get("testGet", Level.INFO);

        assertThat(logger.infoMessageCount, equalTo(0));

        mockTime.set(1L);
        line.log();

        assertThat(logger.infoMessageCount, equalTo(1));

        LogWithPatternAndLevel line2 = rateLimitedLog.get("testGet", Level.INFO);
        assertThat(line2, equalTo(line));
        assertThat(line2, not(equalTo(null)));

        LogWithPatternAndLevel line3 = rateLimitedLog.get("testGet", Level.TRACE);
        assertThat(line3, not(equalTo(line)));
        assertThat(line3, not(equalTo(null)));

        assertThat(line2.hashCode(), equalTo(line.hashCode()));
        assertThat(line2.hashCode(), not(equalTo(line3.hashCode())));
    }

    // Ensure we won't see silly localised formats like "10,000" instead of "10000".
    //
    @Test
    public void testLocaleIgnored() {
        MockLogger logger = new MockLogger();
        final AtomicLong mockTime = new AtomicLong(0L);

        RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(10))
                .withStopwatch(createStopwatch(mockTime))
                .build();

        mockTime.set(1L);
        rateLimitedLog.info("locale {}", 10000);
        assertThat(logger.infoLastMessage.get(), equalTo("locale 10000"));
    }

    // Ensure that the out-of-cache-capacity logic doesn't lose data.
    @Test
    public void outOfCacheCapacity() {
        MockLogger logger = new MockLogger();
        final AtomicLong mockTime = new AtomicLong(0L);

        RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(10))
                .withStopwatch(createStopwatch(mockTime))
                .build();

        for (int i = 0; i < RateLimitedLog.MAX_PATTERNS_PER_LOG + 2; i++) {
            rateLimitedLog.info("cache " + i);
            assertThat(logger.infoLastMessage.get(), equalTo("cache " + i)); // no loss
        }
    }

    private Stopwatch createStopwatch(final AtomicLong mockTime) {
        return Stopwatch.createUnstarted(new Ticker() {
            @Override
            public long read() {
                return mockTime.get();
            }
        });
    }

}
