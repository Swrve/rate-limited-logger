package com.swrve.ratelimitedlogger;

import org.joda.time.Duration;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ManyThreadsTest {

    @Test
    public void manyThreads() throws InterruptedException {
        MockLogger logger = new MockLogger();

        final RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                .maxRate(1).every(Duration.millis(100))
                .build();

        final AtomicBoolean done = new AtomicBoolean(false);

        assertThat(logger.infoMessageCount, equalTo(0));

        ExecutorService exec = Executors.newFixedThreadPool(10);
        for (int thread = 0; thread < 10; thread++) {
            exec.submit(new Runnable() {
                @Override
                public void run() {
                    while (!done.get()) {
                        for (int i = 0; i < 1000; i++) {
                            rateLimitedLog.info("manyThreads {}", Thread.currentThread().getId());
                        }
                    }
                }
            });
        }

        for (int sec = 0; sec < 10; sec++) {
            Thread.sleep(100L);
            logger.info("slept for one sec");
        }

        done.set(true);
        exec.shutdown();
        exec.awaitTermination(60, TimeUnit.SECONDS);

        // now, test that the background suppression-logging thread is still working; the bug previously
        // was that a precondition failure had crashed it
        int c = logger.infoMessageCount;
        Thread.sleep(200L);
        for (int i = 0; i < 10; i++) {
            rateLimitedLog.info("manyThreads 2");
        }
        Thread.sleep(200L);

        // ensure that "similar messages suppressed" took place
        assertThat(logger.infoMessageCount, not(equalTo(c + 10)));
        assertThat(logger.infoLastMessage.get(), startsWith("(suppressed "));

    }
}
