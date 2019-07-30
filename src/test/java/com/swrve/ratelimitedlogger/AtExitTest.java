package com.swrve.ratelimitedlogger;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class AtExitTest {
    private static final Logger logger = LoggerFactory.getLogger(AtExitTest.class);
    private static final RateLimitedLog rateLimitedLog = RateLimitedLog
            .withRateLimit(logger)
            .maxRate(20)
            .every(Duration.ofMinutes(1))
            .build();

    // test for https://github.com/Swrve/rate-limited-logger/issues/11
    @Test
    public void suppressionsOutputPriorToExit() throws InterruptedException {
        for (int i = 0; i < 90; i++) {
            rateLimitedLog.info("Testing: {}", i);
        }
        rateLimitedLog.info("End");
        Thread.sleep(10);
    }
}
