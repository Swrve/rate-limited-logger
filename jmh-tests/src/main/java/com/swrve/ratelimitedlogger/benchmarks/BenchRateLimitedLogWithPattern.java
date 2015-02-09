package com.swrve.ratelimitedlogger.benchmarks;

import com.swrve.ratelimitedlogger.RateLimitedLog;
import com.swrve.ratelimitedlogger.RateLimitedLogWithPattern;
import org.joda.time.Duration;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@State(Scope.Benchmark)
public class BenchRateLimitedLogWithPattern {
    private static final Logger logger = LoggerFactory.getLogger(BenchRateLimitedLogWithPattern.class);
    private static final RateLimitedLog rateLimitedLog = RateLimitedLog.withRateLimit(logger)
                   .maxRate(5).every(Duration.standardSeconds(10))
                   .build();

    private RateLimitedLogWithPattern testMessage;

    @Setup
    public void prepare() {
        // simulate a bunch of unimportant log lines (to fill out the registry)
        for (int i = 0; i < 100; i++) {
            rateLimitedLog.info("unused_" + i);
        }
        testMessage = rateLimitedLog.get("test");
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void testMethod() {
        testMessage.info();
    }
}
