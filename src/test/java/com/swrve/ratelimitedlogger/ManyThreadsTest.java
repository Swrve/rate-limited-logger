package com.swrve.ratelimitedlogger;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import org.joda.time.Duration;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ManyThreadsTest {

    @Test
    public void manyThreads() throws InterruptedException {
        MockLogger logger = new MockLogger();

        final RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(500))
                .build();

        final AtomicBoolean done = new AtomicBoolean(false);

        assertThat(logger.infoMessageCount, equalTo(0));

        ExecutorService tp = Executors.newFixedThreadPool(10);
        for (int thread = 0; thread < 10; thread++) {
            tp.submit(new Runnable() {
                @Override
                public void run() {
                    while (!done.get()) {
                        for (int i = 0; i < 1000; i++) {
                            rateLimitedLog.info("rateLimitingWorksHeavyLoad {}", Thread.currentThread().getId());
                        }
                    }
                }
            });
        }

        // the second message should have been suppressed
        // assertThat(logger.infoMessageCount, equalTo(1));

        for (int sec = 0; sec < 10; sec++) {
            Thread.sleep(1000L);
            logger.info("slept for one sec");
        }

        done.set(true);
        tp.shutdown();
        tp.awaitTermination(60, TimeUnit.SECONDS);
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
